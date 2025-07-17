# Instacart Co-occurrence Analysis

This project implements and benchmarks co-purchase analysis using Apache Spark in Scala. 
It compares the performance and scalability of Spark's RDD and Dataset APIs on a simplified version of the [Instacart Online Grocery Shopping Dataset](https://www.instacart.com/datasets/grocery-shopping-2017).

[Project report available here.](https://andrea-corradetti.github.io/instacart-map-reduce/report/main.pdf)

---

## Project Structure

- `shared/` — Shared data models and utility functions
- `rddApp/` — Co-occurrence analysis using the RDD API
- `datasetApp/` — Co-occurrence analysis using the Dataset API
- `scripts/` — Bash scripts for local/cloud deployment
- `conf/` —  Configuration for local running
- `local/logs` — Default location for logs 

---

## How to Build

This project uses [Mill](https://com-lihaoyi.github.io/mill) as the build tool.

From the project root, build either the RDD or Dataset version with:
```bash
# RDD module
./mill rddApp.assembly

# Dataset module
./mill datasetApp.assembly
```

The compiled JAR will be created under `out/<module>/assembly.dest/out.jar`

## Running locally

```bash
# Dataset Application
spark-submit \
  --class dataset.Main \
  --properties-file config/spark.conf \
  --conf spark.master="local[*]" \
  out/datasetApp/assembly.dest/out.jar \
  --input path/to/order_products.csv \
  --output path/to/output_dir

# Rdd Application
spark-submit \
  --class rdd.Main \
  --properties-file config/spark.conf \
  --conf spark.master="local[*]" \
  out/rddApp/assembly.dest/out.jar \
  --input path/to/order_products.csv \
  --output path/to/output_dir
```

## Running on Google Cloud Dataproc

Note: to use the provided scripts, make a copy of `example.env` in the project root:

```bash
cp example.env .env
```

and fill in the values.

### 1. Create a Bucket with the dataset and upload the dataset
See Google Cloud Storage Docs for [details on creating a bucket](https://cloud.google.com/storage/docs/creating-buckets#command-line).
```Bash
gcloud storage buckets create gs://BUCKET_NAME --location=BUCKET_LOCATION
gcloud storage cp <path/to/order_products.csv> <gs://BUCKET_NAME/destination>
```

### 2. Create a Cluster
See Google Cloud Dataproc docs for [details on creating a cluster](https://cloud.google.com/dataproc/docs/guides/create-cluster#creating_a_cloud_dataproc_cluster).
Alternatively, for a default configuration, from the project root run:

```bash
scripts/cluster.sh create --name <cluster-name> --num-workers <num-workers>
```

### 3. Deploy the jar on the bucket
From the project root, run 
```bash
scripts/deploy.sh --module datasetApp # or rddApp
``` 

### 4. Submit the Job to the Dataproc cluster
See Google Cloud Dataproc [docs for details on submitting a job](https://cloud.google.com/dataproc/docs/guides/submit-job#submitting_a_job).
Alternatively, use the provided script by running:
```bash
scripts/submit-job.sh --cluster <cluster-name>
```

By default, the application expects `order_products.csv` in the bucket root and will produce outputs to `out/latest`:
These can be overridden by running.

```bash
scripts/submit-job.sh --cluster <cluster-name> -- --input <path/to/input> --output <path/to/output>
```

The application will overwrite the destination dir.

### 5. Remember to delete your cluster and bucket
To delete your custer run:
```bash
scripts/cluster.sh delete --name <cluster-name>
```

To delete your bucket, check the [Dataproc Storage docs.](https://cloud.google.com/sdk/gcloud/reference/dataproc/clusters/delete)