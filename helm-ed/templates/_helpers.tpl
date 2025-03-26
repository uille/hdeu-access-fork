{{/*
Expand the name of the chart.
*/}}
{{- define "health-data-access.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "health-data-access.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else }}
{{- include "health-data-access.name" . | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "health-data-access.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | replace "." "_" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "health-data-access.labels" -}}
helm.sh/chart: {{ include "health-data-access.chart" . }}
{{- with .Chart.AppVersion }}
app.kubernetes.io/version: {{ . | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "health-data-access.selectorLabels" -}}
app.kubernetes.io/name: {{ include "health-data-access.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}


{{/*
Create the name of the service account to use
*/}}
{{- define "health-data-access.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "health-data-access.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- "default" }}
{{- end -}}
{{- end -}}
