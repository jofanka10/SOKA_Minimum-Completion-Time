# Minimum Completion Time (MCT) - Cloud Task Scheduling

Implementasi dan ujicoba algoritma **Minimum Completion Time (MCT)** untuk penjadwalan task independen di lingkungan cloud tersimulasi, menggunakan [CloudSim Plus](http://cloudsimplus.org/) 7.3.0. Dibuat untuk mata kuliah SOKA, Tugas Minggu 4.

Dua algoritma pembanding, yaitu FCFS dan Min-Min turut diimplementasikan untuk memperkuat analisis perbandingan.

## **Kelompok 2**

| No | Nama                              | NRP        |
|----|-----------------------------------|------------|
| 1  | Revalina Erica Permatasari        | 5027241007 |
| 2  | Clarissa Aydin Rahmazea           | 5027241014 |
| 3  | Muhammad Afrizan Rasya            | 5027241048 |
| 4  | Angga Firmansyah                  | 5027241062 |
| 5  | Jofanka Al-kautsar Pangestu Abady | 5027241107 |

## **Daftar Isi**

- [Algoritma](#algoritma)
- [Arsitektur Simulasi](#arsitektur-simulasi)
- [Dataset](#dataset)
- [Cara Menjalankan](#cara-menjalankan)
- [Hasil Ujicoba](#hasil-ujicoba)
- [Validasi](#validasi)
- [Struktur Repo](#struktur-repo)
- [Keterbatasan](#keterbatasan)
- [Referensi](#referensi)

## Algoritma

**Minimum Completion Time (MCT)** memproses task satu per satu sesuai urutan kedatangan, dan memetakan setiap task ke VM yang memberikan *completion time* paling awal.

```math
CT(T_i, V_j) = RT(V_j) + ET(T_i, V_j)
```

- $`RT(V_j)`$ : ready time VM j (kapan VM tersebut bebas)
- $`ET(T_i, V_j)`$ : waktu eksekusi task i pada VM j = $\mathrm{length}(T_i) / \mathrm{MIPS}(V_j)$
- Jika beberapa VM memberi CT yang sama, dipilih VM dengan **indeks terkecil**

```
ready[j] = 0 untuk setiap VM j
U = daftar task, terurut sesuai urutan kedatangan

untuk setiap task i di U:
    untuk setiap VM j:
        CT[i][j] = ready[j] + ET(i, j)
    j* = argmin_j( CT[i][j] )     // jika seri: indeks j terkecil
    tugaskan task i ke VM j*
    ready[j*] = CT[i][j*]

makespan = max_j( ready[j] )
```

MCT dipilih sebagai algoritma utama karena (1) memperhitungkan beban VM saat ini sehingga lebih adaptif pada lingkungan heterogen dibanding FCFS, dan (2) kompleksitasnya `O(n·m)` lebih ringan dibanding Min-Min yang `O(n²·m)`.

| Algoritma | Cara memilih | Kompleksitas |
|---|---|---|
| **MCT** (utama) | VM dengan completion time paling awal, per task sesuai urutan | $O(n \cdot m)$ |
| FCFS (baseline) | VM dengan ready time tercepat, tanpa melihat lama eksekusi | $\sim O(n)$ |
| Min-Min (pembanding) | Dari semua task tersisa, pilih completion time minimum paling kecil dulu | $O(n^2 \cdot m)$ |

## Arsitektur Simulasi

Mengikuti spesifikasi Draft Design Project (Tugas Minggu 3):

| Entitas | Jumlah | Spesifikasi |
|---|---|---|
| Datacenter | 1 | `DatacenterSimple`, kebijakan bawaan CloudSim Plus |
| Host | 2 | 8 PE × 100.000 MIPS, RAM 32.000 MB, identik |
| VM | 4 | Small 1.000 / Medium 2.500 / Large 5.000 / XLarge 7.500 MIPS — 1 PE, RAM 4.096 MB, `SpaceShared` |
| Cloudlet | 1.000 | 1 PE per task, `UtilizationModelDynamic(1.0)` |

## Dataset

**GoCJ (Google Cloud Jobs)** — Hussain & Aleem (2018), [Mendeley Data](https://data.mendeley.com/datasets/b7bp6xhrcd/1). Ukuran job dalam Million Instructions (MI), dibangkitkan dari analisis Google cluster trace.

File yang dipakai: `GoCJ_1000_task_simulation.csv` — 1.000 task, hanya kolom `lengthMI` yang dipakai.

| Statistik | Nilai |
|---|---|
| Minimum / Maksimum | 15.000 / 900.000 MI |
| Median / Rata-rata | 89.000 / 121.815 MI |
| Total beban | 121.815.000 MI |
| Batas bawah teoritis makespan | ΣMI / ΣMIPS ≈ **7.613,44 detik** |

## Cara Menjalankan

Prasyarat: Java 17, Maven.

```bash
mvn compile
mvn exec:java -Dexec.mainClass="MctSimulation"
mvn exec:java -Dexec.mainClass="FcfsSimulation"
mvn exec:java -Dexec.mainClass="MinMinSimulation"
```

Setiap run membaca `GoCJ_1000_task_simulation.csv` dan menulis hasil ke `Hasil_Simulasi_<Algoritma>_<timestamp>.csv` berisi `Cloudlet_ID, Status, VM_ID, Actual_CPU_Time, Start_Time, Finish_Time` per task, ditutup satu baris ringkasan makespan.

## Hasil Ujicoba

Seluruh 1.000 cloudlet berstatus **SUCCESS** pada ketiga run.

| Algoritma | Makespan (detik) | Selisih thd batas bawah | Distribusi VM (Small/Medium/Large/XLarge) |
|---|---|---|---|
| **MCT** | **7.690,06** | +1,01% | 137 / 236 / 352 / 275 |
| FCFS | 7.724,00 | +1,46% | 49 / 148 / 328 / 475 |
| Min-Min | 7.781,37 | +2,20% | 61 / 155 / 313 / 471 |

**Peringkat:** MCT < FCFS < Min-Min. Selisih antar-algoritma kecil (<1,2%), ketiganya berada dalam 1-2,2% dari batas bawah teoritis.

Metrik tambahan untuk MCT:

| Metrik | Nilai |
|---|---|
| Degree of Imbalance | 0,011 |
| Resource Utilization | 99,1% |
| Throughput | 0,13 task/detik |

### Temuan menarik: Min-Min sedikit lebih lambat dari FCFS

Berlawanan dengan ekspektasi umum, Min-Min (7.781,37 detik) tampil sedikit di bawah FCFS (7.724,00 detik) pada dataset ini. Ini konsisten dengan kelemahan Min-Min yang terdokumentasi di literatur (Braun et al., 2001): Min-Min cenderung memprioritaskan task kecil berturut-turut ke VM tercepat, sehingga VM tersebut menumpuk banyak task. Dataset GoCJ bersifat *right-skewed* (919 dari 1.000 task ≤ 135.000 MI, 63 task > 500.000 MI), yang kemungkinan memperkuat efek ini.

## Validasi

Setiap algoritma direplikasi manual (Python, logika identik dengan kode Java) dan dibandingkan dengan hasil CloudSim Plus:

| Algoritma | Manual | CloudSim Plus | Selisih |
|---|---|---|---|
| MCT | 7.634,80 | 7.690,06 | 0,72% |
| FCFS | 7.628,47 | 7.724,00 | 1,25% |
| Min-Min | 7.686,67 | 7.781,37 | 1,23% |

Selisih 0,7-1,3% konsisten di ketiganya, sejalan dengan jeda minimum antar-event bawaan CloudSim Plus (`minTimeBetweenEvents` = 0,1 detik). Khusus MCT, pemetaan task→VM hasil hitungan manual dibandingkan satu per satu dengan keluaran simulator: **kecocokan 100%**.

## Struktur Repo

```
├── src/main/java/
│   ├── MctSimulation.java      # algoritma utama
│   ├── FcfsSimulation.java     # baseline
│   └── MinMinSimulation.java   # pembanding
├── GoCJ_1000_task_simulation.csv        # dataset
├── Hasil_Simulasi_MCT_*.csv             # hasil run MCT
├── Hasil_Simulasi_FCFS_*.csv            # hasil run FCFS
├── Hasil_Simulasi_MinMin_*.csv          # hasil run Min-Min
└── pom.xml
```

## Keterbatasan

- **Total cost (F2)** belum dihitung untuk ketiga algoritma; baru makespan (F1), imbalance (F3), utilization, dan throughput yang terukur lengkap (khusus MCT).
- Skenario ukuran dataset lebih kecil (n = 100, 200, 400, 800) belum dijalankan.
- Konfigurasi homogen (4 VM identik) belum dijalankan.
- Waktu komputasi algoritma belum diukur secara eksplisit.
- Ujicoba real-world (eksekusi task pada resource nyata) belum dilakukan.

## Referensi

- Braun, T.D., Siegel, H.J., Beck, N., Bölöni, L.L., Maheswaran, M., et al. (2001). A comparison of eleven static heuristics for mapping a class of independent tasks onto heterogeneous distributed computing systems. *Journal of Parallel and Distributed Computing*, 61(6), 810–837.
- Calheiros, R.N., Ranjan, R., Beloglazov, A., De Rose, C.A.F., Buyya, R. (2011). CloudSim: a toolkit for modeling and simulation of cloud computing environments and evaluation of resource provisioning algorithms. *Software: Practice and Experience*, 41(1), 23–50.
- Hussain, A., Aleem, M. (2018). GoCJ: Google Cloud Jobs Dataset for Distributed and Cloud Computing Infrastructures. *Data*, 3(4), 38. https://doi.org/10.3390/data3040038
- Silva Filho, M.C., Oliveira, R.L., Monteiro, C.C., Inácio, P.R.M., Freire, M.M. (2017). CloudSim Plus: a cloud computing simulation framework pursuing software engineering principles for improved modularity, extensibility and correctness. *IFIP/IEEE IM*, Lisbon.
