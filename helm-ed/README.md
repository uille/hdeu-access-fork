# HDEU Access Project Helm Charts

## Table of Contents
- [HDEU Access Project Helm-Charts](#hdeu-access-project-helm-charts)
- [Kubernetes Version Support](#kubernetes-version-support)
- [Installation](#installation)
- [Dependencies](#dependencies)
  - [Running Outside AWS](#running-outside-aws)
- [Configuration](#configuration)
- [AWS Secrets Manager Integration](#aws-secrets-manager-integration)
  - [How It Works](#how-it-works)
  - [SecretProviderClass Definition](#secretproviderclass-definition)
  - [Mapping AWS Secrets to Kubernetes Secrets](#mapping-aws-secrets-to-kubernetes-secrets)
  - [How to Use](#how-to-use)
- [License](#license)

## HDEU Access Project Helm Charts

A community repository for Helm Charts of HDEU Access Project.

## Kubernetes Version Support

* This Helm chart repository is tested with Kubernetes version 1.29 and above.

## Dependencies

This Helm chart requires a MongoDB database for storing application data. The database configuration depends on the deployment environment:

### **Running Outside AWS**
When deploying in AWS, the application does not require a separate MongoDB installation because it integrates with AWS DocumentDB using AWS Secrets Manager.
When deploying outside AWS, an instance of MongoDB **must be installed and accessible** within the Kubernetes cluster. The application requires the following MongoDB configuration:

| Configuration Key       | Description                                   | Example Value |
|-------------------------|-----------------------------------------------|--------------|
| `config.MONGO_DB_URI`   | MongoDB connection string                     | `mongodb://mongodb.default.svc.cluster.local:27017` |
| `config.MONGO_DB`       | MongoDB database name                         | `hdeu-DataAccessDB` |
| `config.MONGO_USER`     | MongoDB username (if authentication enabled)  | `admin` |
| `config.MONGO_PW`       | MongoDB password (if authentication enabled)  | `password` |

To set up MongoDB, you can use a Helm chart like Bitnami’s MongoDB:

```shell
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install my-mongo bitnami/mongodb --set auth.enabled=true
```


## Installation

To install via Helm, run the following command:

```shell
helm upgrade --install release-name . -f values.yaml -n default
```

## Configuration

The Helm chart can be customized using the `values.yaml` file. Modify this file to set parameters according to your requirements before installation.

You can also override values at the time of installation using the `--set` flag. For example:

```shell
helm upgrade --install release-name . -f values.yaml --set key=value -n default
```

For a full list of configurable parameters, refer to `values.yaml`.

## AWS Secrets Manager Integration

This Helm chart supports integration with **AWS Secrets Manager** using the **Secrets Store CSI Driver** to securely retrieve secrets and expose them as Kubernetes secrets.

### How It Works

- AWS Secrets Manager stores sensitive configuration values.
- The **SecretProviderClass** defines which secrets to retrieve and how they are mapped.
- The retrieved secrets are automatically created as **Kubernetes secrets** and mounted into the application as environment variables.

### SecretProviderClass Definition

The following **SecretProviderClass** is created when `aws.enabled` is set to `true`:

```yaml
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: {{ include "health-data-access.name" . }}-spc
spec:
  provider: {{ .Values.secrets.provider }}
  parameters:
    objects: |
      - objectName: "HEALTHDATA_MONGO_DB_SECRET"
        objectType: "secretsmanager"
        jmesPath:
          - path: username
            objectAlias: username
          - path: password
            objectAlias: password
          - path: mongo_uri
            objectAlias: mongo_uri
      - objectName: "HEALTHDATA_API_KEYS"
        objectType: "secretsmanager"
        jmesPath:
          - path: hub_store_data_upload_api_key
            objectAlias: hub_store_data_upload_api_key
          - path: health_data_access_api_key
            objectAlias: health_data_access_api_key
          - path: dispatcher_x_api_key
            objectAlias: dispatcher_x_api_key
          - path: export_data_access_api_key
            objectAlias: export_data_access_api_key
      - objectName: "HEALTH_DATA_ACCESS_SECRETS"
        objectType: "secretsmanager"
        jmesPath:
          - path: access_auth_config
            objectAlias: access_auth_config
  secretObjects:
    - secretName:  {{ include "health-data-access.name" . }}-secrets
      type: Opaque
      data:
        - objectName: username
          key: username
        - objectName: password
          key: password
        - objectName: mongo_uri
          key: mongo_uri
        - objectName: hub_store_data_upload_api_key
          key: hub_store_data_upload_api_key
        - objectName: health_data_access_api_key
          key: health_data_access_api_key
        - objectName: dispatcher_x_api_key
          key: dispatcher_x_api_key
        - objectName: access_auth_config
          key: access_auth_config
        - objectName: export_data_access_api_key
          key: export_data_access_api_key
```

### Mapping AWS Secrets to Kubernetes Secrets

| AWS Secret Name                       | AWS Key (path)                      | Kubernetes Secret Key            |
|---------------------------------------|-------------------------------------|----------------------------------|
| `HEALTHDATA_MONGO_DB_SECRET`          | `username`                          | `username`                       |
|                                       | `password`                          | `password`                       |
|                                       | `mongo_uri`                         | `mongo_uri`                      |
| `HEALTHDATA_API_KEYS`                 | `hub_store_data_upload_api_key`     | `hub_store_data_upload_api_key`  |
|                                       | `health_data_access_api_key`        | `health_data_access_api_key`     |
|                                       | `dispatcher_x_api_key`              | `dispatcher_x_api_key`           |
|                                       | `export_data_access_api_key`        | `export_data_access_api_key`     |
| `HEALTH_DATA_ACCESS_SECRETS`          | `access_auth_config`                | `access_auth_config`             |


### How to Use

1. Ensure that **AWS Secrets Manager** is configured with the required secrets.
2. Set `aws.enabled: true` in `values.yaml`.
3. Deploy the Helm chart.
4. The application will retrieve secrets automatically via the CSI driver.

For more details, refer to:

- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/latest/userguide/intro.html)
- [Secrets Store CSI Driver Documentation](https://docs.aws.amazon.com/secretsmanager/latest/userguide/integrating_csi_driver.html)

## License

This project is licensed under the [EUROPEAN UNION PUBLIC LICENCE v. 1.2](https://eupl.eu/1.2/en/)
