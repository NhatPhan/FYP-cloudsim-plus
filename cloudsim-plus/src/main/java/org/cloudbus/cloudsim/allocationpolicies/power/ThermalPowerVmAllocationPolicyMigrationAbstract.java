package org.cloudbus.cloudsim.allocationpolicies.power;

import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostDynamicWorkload;
import org.cloudbus.cloudsim.hosts.power.PowerHost;
import org.cloudbus.cloudsim.hosts.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.selectionpolicies.power.PowerVmSelectionPolicy;
import org.cloudbus.cloudsim.thermal.HotspotApi;
import org.cloudbus.cloudsim.util.Log;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public abstract class ThermalPowerVmAllocationPolicyMigrationAbstract extends PowerVmAllocationPolicyAbstract
    implements ThermalPowerVmAllocationPolicyMigration {

    private double underUtilizationThreshold;
    private double thresholdTemperature;

    private PowerVmSelectionPolicy vmSelectionPolicy;

    private final Map<Vm, Host> savedAllocation;
    private final Map<Host, List<Double>> utilizationHistory;
    private final Map<Host, List<Double>> metricHistory;
    private final Map<Host, List<Double>> timeHistory;

    public ThermalPowerVmAllocationPolicyMigrationAbstract(PowerVmSelectionPolicy vmSelectionPolicy)
    {
        super();
        this.underUtilizationThreshold = 0.35;
        this.savedAllocation = new HashMap<>();
        this.utilizationHistory = new HashMap<>();
        this.metricHistory = new HashMap<>();
        this.timeHistory = new HashMap<>();
        setVmSelectionPolicy(vmSelectionPolicy);
    }

    protected PowerVmSelectionPolicy getVmSelectionPolicy() {
        return vmSelectionPolicy;
    }

    @Override
    public Map<Vm, Host> optimizeAllocation(List<? extends Vm> vmList) {
        final Set<PowerHostUtilizationHistory> overloadedOrOverThresholdTemperatureHosts =
            getOverloadedOrOverThresholdTemperatureHosts();

        saveAllocation();

        final Map<Vm, Host> migrationMap =
            getMigrationMapFromOverLoadedOrOverThresholdTemperatureHosts(overloadedOrOverThresholdTemperatureHosts);

        updateMigrationMapFromUnderloadedHosts(overloadedOrOverThresholdTemperatureHosts, migrationMap);
        restoreAllocation();
        return migrationMap;
    }

    /**
     * Sets the vm selection policy.
     */
    protected final void setVmSelectionPolicy(PowerVmSelectionPolicy vmSelectionPolicy) {
        this.vmSelectionPolicy = vmSelectionPolicy;
    }

    protected Set<PowerHostUtilizationHistory> getOverloadedOrOverThresholdTemperatureHosts() {
        Set<PowerHostUtilizationHistory> overloadedOrOverThresholdTemperatureHosts = new HashSet<>();

        Set<PowerHostUtilizationHistory> overloadedHosts = getOverloadedHosts();
        printOverUtilizedHosts(overloadedHosts);

        Set<PowerHostUtilizationHistory> overThresholdTemperatureHosts = getOverThresholdTemperatureHosts();
        printOverThresholdTemperatureHosts(overThresholdTemperatureHosts);

        overloadedOrOverThresholdTemperatureHosts.addAll(getOverloadedHosts());
        overloadedOrOverThresholdTemperatureHosts.addAll(getOverThresholdTemperatureHosts());

        return overloadedOrOverThresholdTemperatureHosts;
    }

    protected Set<PowerHostUtilizationHistory> getOverloadedHosts() {
        return this.<PowerHostUtilizationHistory>getHostList().stream()
            .filter(this::isHostOverloaded)
            .filter(h -> h.getVmsMigratingOut().isEmpty())
            .collect(toSet());
    }

    protected Set<PowerHostUtilizationHistory> getOverThresholdTemperatureHosts() {
        return this.<PowerHostUtilizationHistory>getHostList().stream()
            .filter(this::isHostOverThresholdTemperature)
            .filter(h -> h.getVmsMigratingOut().isEmpty())
            .collect(toSet());
    }

    private void printOverUtilizedHosts(Set<PowerHostUtilizationHistory> overloadedHosts) {
        if (!Log.isDisabled() && !overloadedHosts.isEmpty()) {
            Log.printFormattedLine("%.2f: PowerVmAllocationPolicy: Overloaded hosts in %s: %s",
                getDatacenter().getSimulation().clock(), getDatacenter(),
                overloadedHosts.stream().map(h -> String.valueOf(h.getId())).collect(joining(",")));
        }
    }

    private void printOverThresholdTemperatureHosts(Set<PowerHostUtilizationHistory> overThresholdTemperatureHosts) {
        if (!Log.isDisabled() && !overThresholdTemperatureHosts.isEmpty()) {
            Log.printFormattedLine("%.2f: PowerVmAllocationPolicy: Over Threshold Temperature hosts in %s: %s",
                getDatacenter().getSimulation().clock(), getDatacenter(),
                overThresholdTemperatureHosts.stream().map(h -> String.valueOf(h.getId())).collect(joining(",")));
        }
    }

    /**
     * Saves the current map between a VM and the host where it is place.
     */
    protected void saveAllocation() {
        savedAllocation.clear();
        for (final Host host : getHostList()) {
            for (final Vm vm : host.getVmList()) {
                if (!host.getVmsMigratingIn().contains(vm)) {
                    savedAllocation.put(vm, host);
                }
            }
        }
    }

    /**
     * Gets a new VM placement considering the list of VM to migrate
     * from overloaded Hosts.
     */
    protected Map<Vm, Host> getMigrationMapFromOverLoadedOrOverThresholdTemperatureHosts
        (final Set<PowerHostUtilizationHistory> hosts)
    {
        final List<Vm> vmsToMigrate = getVmsToMigrateFromOverLoadedOrOverThresholdTemperatureHosts(hosts);
        final Map<Vm, Host> migrationMap = new HashMap<>();

        if(hosts.isEmpty()) { return migrationMap; }

        Log.printLine("\tReallocation of VMs from overloaded hosts: ");

        VmList.sortByCpuUtilization(vmsToMigrate, getDatacenter().getSimulation().clock());

        for (final Vm vm : vmsToMigrate) {
            final PowerHost targetHost = findHostForVm(vm, hosts);
            if (targetHost != PowerHost.NULL) {
                /* Temporarily creates the VM to be migrated from the overloaded Host into
                 * the selected target Host so that when the a Host is selected for
                 * the next VM, if the current selected Host doesn't fit another VM,
                 * it will not be selected anymore. */
                targetHost.createTemporaryVm(vm);

                Log.printConcatLine("\tVM #", vm.getId(), " will be migrated to host #", targetHost.getId());
                migrationMap.put(vm, targetHost);
            }
        }
        Log.printLine();

        return migrationMap;
    }

    /**
     * Gets the VMs to migrate from Hosts.
     */
    protected List<Vm> getVmsToMigrateFromOverLoadedOrOverThresholdTemperatureHosts
        (Set<PowerHostUtilizationHistory> hosts)
    {
        final List<Vm> vmsToMigrate = new LinkedList<>();

        for (final PowerHostUtilizationHistory host : hosts) {
            vmsToMigrate.addAll(getVmsToMigrateFromOverLoadedOrOverThresholdTemperatureHost(host));
        }

        return vmsToMigrate;
    }

    private List<Vm> getVmsToMigrateFromOverLoadedOrOverThresholdTemperatureHost(PowerHostUtilizationHistory host)
    {
        final List<Vm> vmsToMigrate = new LinkedList<>();

        while (true) {
            final Vm vm = getVmSelectionPolicy().getVmToMigrate(host);

            if (Vm.NULL == vm) { break; }

            vmsToMigrate.add(vm);

            /*Temporarily destroys the selected VM into the overloaded Host so that
            the loop gets VMs from such a Host until it is not overloaded anymore.*/
            host.destroyTemporaryVm(vm);

            if (!isHostOverloaded(host) && !isHostOverThresholdTemperature(host)) { break; }
        }

        return vmsToMigrate;
    }

    public PowerHost findHostForVm(final Vm vm, final Set<? extends Host> excludedHosts) {
        /*The predicate also returns true to indicate that in fact it is not
        applying any additional filter.*/
        return findHostForVm(vm, excludedHosts, host -> true);
    }

    public PowerHost findHostForVm(final Vm vm, final Set<? extends Host> excludedHosts, Predicate<PowerHost> predicate) {
        final Stream<PowerHost> stream = this.<PowerHost>getHostList().stream()
            .filter(h -> !excludedHosts.contains(h))
            .filter(h -> h.isSuitableForVm(vm))
            .filter(h -> isNotHostOverloadedAfterAllocation(h, vm))
            .filter(h -> isNotHostOverThresholdTemperatureAfterAllocation(h, vm))
            .filter(predicate);

        return findHostForVmInternal(vm, stream).orElse(PowerHost.NULL);
    }

    protected boolean isNotHostOverloadedAfterAllocation(PowerHost host, Vm vm) {
        boolean isHostOverUsedAfterAllocation = true;

        if (host.createTemporaryVm(vm)) {
            isHostOverUsedAfterAllocation = isHostOverloaded(host);
            host.destroyTemporaryVm(vm);
        }

        return !isHostOverUsedAfterAllocation;
    }

    protected boolean isNotHostOverThresholdTemperatureAfterAllocation(PowerHost host, Vm vm) {
        boolean isHostOverUsedAfterAllocation = true;

        if (host.createTemporaryVm(vm)) {
            isHostOverUsedAfterAllocation = isHostOverThresholdTemperature(host);
            host.destroyTemporaryVm(vm);
        }

        return !isHostOverUsedAfterAllocation;
    }

    public boolean isHostOverloaded(PowerHost host) {
        final double upperThreshold = getOverUtilizationThreshold(host);
        addHistoryEntryIfAbsent(host, upperThreshold);

        return getHostCpuUtilizationPercentage(host) > upperThreshold;
    }

    public boolean isHostOverThresholdTemperature(PowerHost host) {
        final double upperThreshold = getThresholdTemperature(host);
        addHistoryEntryIfAbsent(host, upperThreshold);

        return getHostCpuTemperature(host) > upperThreshold;
    }

    protected Optional<PowerHost> findHostForVmInternal(final Vm vm, final Stream<PowerHost> hostStream){
        final Comparator<PowerHost> hostPowerConsumptionComparator =
            Comparator.comparingDouble(h -> getPowerAfterAllocationDifference(h, vm));

        return additionalHostFilters(vm, hostStream).min(hostPowerConsumptionComparator);
    }

    protected Stream<PowerHost> additionalHostFilters(final Vm vm, final Stream<PowerHost> hostStream){
        return hostStream.filter(h -> getPowerAfterAllocation(h, vm) > 0);
    }

    protected double getPowerAfterAllocationDifference(PowerHost host, Vm vm){
        final double powerAfterAllocation = getPowerAfterAllocation(host, vm);

        if (powerAfterAllocation > 0) {
            return powerAfterAllocation - host.getPower();
        }

        return 0;
    }

    protected double getPowerAfterAllocation(PowerHost host, Vm vm) {
        try {
            return host.getPowerModel().getPower(getMaxUtilizationAfterAllocation(host, vm));
        } catch (Exception e) {
            Log.printFormattedLine("[ERROR] Power consumption for Host %d could not be determined: ", host.getId(), e.getMessage());
        }

        return 0;
    }

    protected double getMaxUtilizationAfterAllocation(PowerHost host, Vm vm) {
        final double requestedTotalMips = vm.getCurrentRequestedTotalMips();
        final double hostUtilizationMips = getUtilizationOfCpuMips(host);
        final double hostPotentialMipsUse = hostUtilizationMips + requestedTotalMips;
        return hostPotentialMipsUse / host.getTotalMipsCapacity();
    }

    protected double getUtilizationOfCpuMips(PowerHost host) {
        double hostUtilizationMips = 0;
        for (final Vm vm2 : host.getVmList()) {
            if (host.getVmsMigratingIn().contains(vm2)) {
                // calculate additional potential CPU usage of a migrating in VM
                hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2) * 0.9 / 0.1;
            }
            hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2);
        }
        return hostUtilizationMips;
    }

    protected void addHistoryEntryIfAbsent(PowerHost host, double metric) {
        timeHistory.putIfAbsent(host, new LinkedList<>());
        utilizationHistory.putIfAbsent(host, new LinkedList<>());
        metricHistory.putIfAbsent(host, new LinkedList<>());

        final Simulation simulation = host.getSimulation();
        if (!timeHistory.get(host).contains(simulation.clock())) {
            timeHistory.get(host).add(simulation.clock());
            utilizationHistory.get(host).add(host.getUtilizationOfCpu());
            metricHistory.get(host).add(metric);
        }
    }

    private double getHostCpuUtilizationPercentage(PowerHost host) {
        return getHostTotalRequestedMips(host) / host.getTotalMipsCapacity();
    }

    private double getHostTotalRequestedMips(PowerHost host) {
        return host.getVmList().stream()
            .mapToDouble(Vm::getCurrentRequestedTotalMips)
            .sum();
    }

    private double getHostCpuTemperature(PowerHost host) {
        //TODO: Add API to get data from Hotspot
        //Random rand = new Random();
        //return rand.nextDouble() * 500 + 10.1;
        //System.out.println("result from hotspot: " + HotspotApi.getTemperature(host.getPower()));
        return HotspotApi.getTemperature(host.getPower());
    }

    private void updateMigrationMapFromUnderloadedHosts(Set<PowerHostUtilizationHistory> overloadedHosts, final Map<Vm, Host> migrationMap) {
        final List<PowerHost> switchedOffHosts = getSwitchedOffHosts();

        // over-utilized hosts + hosts that are selected to migrate VMs to from over-utilized hosts
        final Set<Host> excludedHostsFromUnderloadSearch = new HashSet<>();
        excludedHostsFromUnderloadSearch.addAll(overloadedHosts);
        excludedHostsFromUnderloadSearch.addAll(switchedOffHosts);
        /*
        During the computation of the new placement for VMs
        the current VM placement is changed temporarily, before the actual migration of VMs.
        If VMs are being migrated from overloaded Hosts, they in fact already were removed
        from such Hosts and moved to destination ones.
        The target Host that maybe were shut down, might become underloaded too.
        This way, such Hosts are added to be ignored when
        looking for underloaded Hosts.
        See https://github.com/manoelcampos/cloudsim-plus/issues/94
         */
        excludedHostsFromUnderloadSearch.addAll(migrationMap.values());

        // over-utilized + under-utilized hosts
        final Set<PowerHost> excludedHostsForFindingNewVmPlacement = new HashSet<>();
        excludedHostsForFindingNewVmPlacement.addAll(overloadedHosts);
        excludedHostsForFindingNewVmPlacement.addAll(switchedOffHosts);

        final int numberOfHosts = getHostList().size();

        while (true) {
            if (numberOfHosts == excludedHostsFromUnderloadSearch.size()) {
                break;
            }

            final PowerHost underloadedHost = getUnderloadedHost(excludedHostsFromUnderloadSearch);
            if (underloadedHost == PowerHost.NULL) {
                break;
            }

            Log.printFormattedLine("%.2f: PowerVmAllocationPolicy: Underloaded hosts: %s", getDatacenter().getSimulation().clock(),  underloadedHost);

            excludedHostsFromUnderloadSearch.add(underloadedHost);
            excludedHostsForFindingNewVmPlacement.add(underloadedHost);

            List<? extends Vm> vmsToMigrateFromUnderloadedHost = getVmsToMigrateFromUnderUtilizedHost(underloadedHost);
            if (!vmsToMigrateFromUnderloadedHost.isEmpty()) {
                Log.printFormatted("\tVMs to be reallocated from the underloaded Host %d: ", underloadedHost.getId());
                printVmIds(vmsToMigrateFromUnderloadedHost);

                final Map<Vm, Host> newVmPlacement = getNewVmPlacementFromUnderloadedHost(
                    vmsToMigrateFromUnderloadedHost,
                    excludedHostsForFindingNewVmPlacement);

                excludedHostsFromUnderloadSearch.addAll(extractHostListFromMigrationMap(newVmPlacement));
                migrationMap.putAll(newVmPlacement);
                Log.printLine();
            }
        }
    }

    private PowerHost getUnderloadedHost(Set<? extends Host> excludedHosts) {
        return this.<PowerHost>getHostList().stream()
            .filter(h -> !excludedHosts.contains(h))
            .filter(h -> h.getUtilizationOfCpu() > 0)
            .filter(this::isHostUnderloaded)
            .filter(h -> h.getVmsMigratingIn().isEmpty())
            .filter(this::isNotAllVmsMigratingOut)
            .min(Comparator.comparingDouble(HostDynamicWorkload::getUtilizationOfCpu))
            .orElse(PowerHost.NULL);
    }

    protected boolean isNotAllVmsMigratingOut(PowerHost host) {
        return host.getVmList().stream().anyMatch(vm -> !vm.isInMigration());
    }

    protected List<? extends Vm> getVmsToMigrateFromUnderUtilizedHost(PowerHost host) {
        return host.getVmList().stream()
            .filter(vm -> !vm.isInMigration())
            .collect(Collectors.toCollection(LinkedList::new));
    }

    private void printVmIds(List<? extends Vm> vmList) {
        if (!Log.isDisabled()) {
            vmList.forEach(vm -> Log.printFormatted("Vm %d ", vm.getId()));
            Log.printLine();
        }
    }

    protected List<PowerHost> getSwitchedOffHosts() {
        return this.<PowerHost>getHostList().stream()
            .filter(host -> !host.isActive() || host.isFailed())
            .collect(toList());
    }

    protected Map<Vm, Host> getNewVmPlacementFromUnderloadedHost(
        final List<? extends Vm> vmsToMigrate,
        final Set<? extends Host> excludedHosts)
    {
        final Map<Vm, Host> migrationMap = new HashMap<>();
        VmList.sortByCpuUtilization(vmsToMigrate, getDatacenter().getSimulation().clock());
        for (final Vm vm : vmsToMigrate) {
            //try to find a target Host to place a VM from an underloaded Host that is not underloaded too
            final PowerHost targetHost = findHostForVm(vm, excludedHosts, host -> !isHostUnderloaded(host));
            if (PowerHost.NULL == targetHost) {
                Log.printFormattedLine("\tA new Host, which isn't also underloaded or won't be overloaded, couldn't be found to migrate %s.", vm);
                Log.printFormattedLine("\tMigration of VMs from the underloaded %s cancelled.", vm.getHost());
                return new HashMap<>();
            } else {
                /*
                Temporarily creates the Host into the target Host so that
                when the next VM is got to be migrated, if the same Host
                is selected as destination, the resource to be
                used by the previous VM will be considering when
                assessing the suitability of such a Host for the next VM.
                 */
                targetHost.createTemporaryVm(vm);
                Log.printConcatLine("\tVM #", vm.getId(), " will be allocated to host #", targetHost.getId());
                migrationMap.put(vm, targetHost);
            }
        }

        return migrationMap;
    }

    protected List<Host> extractHostListFromMigrationMap(Map<Vm, Host> migrationMap) {
        return migrationMap.entrySet().stream()
            .map(Map.Entry::getValue)
            .collect(toList());
    }

    protected void restoreAllocation() {
        for (final Host host : getHostList()) {
            host.destroyAllVms();
            host.reallocateMigratingInVms();
        }

        for (final Vm vm : savedAllocation.keySet()) {
            final PowerHost host = (PowerHost) savedAllocation.get(vm);
            if (!host.createTemporaryVm(vm)) {
                Log.printFormattedLine(
                    "Couldn't restore VM #%d on Host #%d",
                    vm.getId(), host.getId());
                return;
            }
        }
    }

    @Override
    public Map<Host, List<Double>> getUtilizationHistory() {
        return Collections.unmodifiableMap(utilizationHistory);
    }

    @Override
    public Map<Host, List<Double>> getMetricHistory() {
        return Collections.unmodifiableMap(metricHistory);
    }

    @Override
    public Map<Host, List<Double>> getTimeHistory() {
        return Collections.unmodifiableMap(timeHistory);
    }

    @Override
    public double getUnderUtilizationThreshold() {
        return underUtilizationThreshold;
    }

    @Override
    public void setUnderUtilizationThreshold(double underUtilizationThreshold) {
        this.underUtilizationThreshold = underUtilizationThreshold;
    }

    @Override
    public boolean isHostUnderloaded(PowerHost host) {
        return getHostCpuUtilizationPercentage(host) < getUnderUtilizationThreshold();
    }
}
