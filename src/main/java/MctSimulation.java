import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MctSimulation {

    public static void main(String[] args) {
        // 1. Inisialisasi CloudSim Plus
        CloudSim simulation = new CloudSim();

        // 2. Buat Host dan Datacenter
        List<Host> hostList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < 8; j++) peList.add(new PeSimple(100000));
            Host host = new HostSimple(32000, 1000000, 1000000, peList);
            hostList.add(host);
        }
        Datacenter datacenter = new DatacenterSimple(simulation, hostList);

        // 3. Buat Broker & Virtual Machines
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);
        List<Vm> vmList = new ArrayList<>();
        double[] mipsCapacities = {1000, 2500, 5000, 7500};

        for (int i = 0; i < mipsCapacities.length; i++) {
            Vm vm = new VmSimple(mipsCapacities[i], 1);
            vm.setRam(4096).setBw(10000).setSize(10000);
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            vmList.add(vm);
        }
        broker.submitVmList(vmList);

        // 4. Baca CSV dan Konversi menjadi Cloudlet
        List<Cloudlet> cloudletList = new ArrayList<>();
        String csvFile = "GoCJ_1000_task_simulation.csv";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }

                String[] values = line.split(",");
                long lengthMI = Long.parseLong(values[1]);

                Cloudlet cloudlet = new CloudletSimple(lengthMI, 1)
                    .setUtilizationModel(new UtilizationModelDynamic(1.0));
                cloudletList.add(cloudlet);
            }
        } catch (IOException e) {
            System.out.println("Gagal membaca CSV. Pastikan file GoCJ_1000_task_simulation.csv ada di root folder.");
            return;
        }

        // 5. Algoritma Penjadwalan MCT (Minimum Completion Time)
        double[] vmReadyTimes = new double[vmList.size()];

        for (Cloudlet cloudlet : cloudletList) {
            int bestVmIndex = 0;
 
            double minCompletionTime = Double.MAX_VALUE;

            for (int i = 0; i < vmList.size(); i++) {
                Vm vm = vmList.get(i);
                double executionTime = cloudlet.getLength() / vm.getMips();
                double completionTime = vmReadyTimes[i] + executionTime;

                if (completionTime < minCompletionTime) {
                    minCompletionTime = completionTime;
                    bestVmIndex = i;
                }
            }

            Vm selectedVm = vmList.get(bestVmIndex);
            cloudlet.setVm(selectedVm);
            vmReadyTimes[bestVmIndex] = minCompletionTime;
        }

        // 6. Submit dan Jalankan
        broker.submitCloudletList(cloudletList);
        System.out.println("Memulai Simulasi MCT di CloudSim Plus...");
        simulation.start();

        // 7. Cetak Tabel Hasil
        new CloudletsTableBuilder(broker.getCloudletFinishedList()).build();
    }
}