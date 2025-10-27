# Table of Contents

- [Overview](#overview)
- [Scope](#scope)
- [ Prerequisites](#️-prerequisites)
- [Steps to Upgrade the EKS Cluster](#steps-to-upgrade-the-eks-cluster)

#  EKS Cluster Upgrade Playbook

##  Overview

This document provides a step-by-step guide to upgrading an **Amazon Elastic Kubernetes Service (EKS)** cluster, including:

- EKS **control plane**
- **EKS-managed add-ons**
- **Worker nodes** (Managed Node Groups or Self-managed nodes)
- **Validation and rollback steps**

The goal is to ensure a **safe**, **controlled**, and **reversible** upgrade process.

---

##  Scope

The playbook covers:

- Upgrade **EKS control plane** (Kubernetes version bump)
- Upgrade core EKS add-ons:
  - `vpc-cni`
  - `kube-proxy`
  - `coredns`
- Upgrade **Worker Nodes** (Managed Node Groups / Self-managed)
- Validation and Rollback Procedures

---

##  Prerequisites

Before starting the upgrade, ensure the following:

###  1. Check Current Versions
```bash
aws eks describe-cluster --name <cluster_name> --query "cluster.version" --output text
```
### 2. Update Required Tools

Ensure the following tools are updated to the latest versions:

- aws-cli

- eksctl

- kubectl

- helm (if using Helm-managed workloads)

### 3. Backup Important Resources

Take a backup of critical cluster configurations:
```
kubectl get all --all-namespaces -o yaml > cluster-backup.yaml
```

### 4. Review Supported Versions

- AWS supports only 3 minor Kubernetes versions at a time.

- Upgrade one minor version at a time (e.g., 1.26 → 1.27, not 1.26 → 1.28).

### 5. Verify IAM Permissions

Ensure your IAM role has the following permissions:

- eks:UpdateClusterVersion

- eks:DescribeCluster

- eks:UpdateNodegroupVersion
# Steps to upgrade the EKS Cluster
## Step 1: Review Cluster and Node Versions
```
aws eks describe-cluster --name <cluster_name> --query "cluster.version" --output text
kubectl version --short
```
List managed node groups:
```
aws eks list-nodegroups --cluster-name <cluster_name>
```
Describe each node group:
```
aws eks describe-nodegroup --cluster-name <cluster_name> --nodegroup-name <node_group_name> --query "nodegroup.version"
```
## Step 2: Upgrade the Control Plane
Upgrade to the next Kubernetes version:
```
aws eks update-cluster-version --name <cluster_name> --kubernetes-version <target_version>
```
Example:
```
aws eks update-cluster-version --name dev-eks --kubernetes-version 1.28
```
Check upgrade status:
```
aws eks describe-cluster --name <cluster_name> --query "cluster.status"
```

Wait until status = ACTIVE.

## Step 3: Upgrade Core EKS Add-ons
### 3.1. Amazon VPC CNI
```
eksctl utils update-cni --cluster <cluster_name> --approve
```
Or via console:
```
aws eks update-addon --cluster-name <cluster_name> --addon-name vpc-cni
```
### 3.2. CoreDNS
```
aws eks update-addon --cluster-name <cluster_name> --addon-name coredns
```
Check rollout:
```
kubectl get pods -n kube-system -l k8s-app=kube-dns
```
### 3.3. kube-proxy
```
aws eks update-addon --cluster-name <cluster_name> --addon-name kube-proxy
```
## Step 4: Upgrade Worker Nodes
### 4.1. For Managed Node Groups
```
aws eks update-nodegroup-version --cluster-name <cluster_name> --nodegroup-name <node_group_name>
```
Monitor upgrade:
```
aws eks describe-nodegroup --cluster-name <cluster_name> --nodegroup-name <node_group_name> --query "nodegroup.status"
```
Wait until status = ACTIVE.

### 4.2. For Self-Managed Nodes

- Update your node AMI ID in the Auto Scaling Group (ASG).

- Launch new instances with updated AMI.

- Drain and terminate old nodes:

```
kubectl drain <node_name> --ignore-daemonsets --delete-emptydir-data
kubectl delete node <node_name>
```

## Step 5: Post-Upgrade Verification
1. Verify cluster and node versions:
```
kubectl version --short
kubectl get nodes -o wide
```
2. Check cluster components:
```
kubectl get pods -A
```
3. Check workloads and services:
```
kubectl get deployments -A
kubectl get svc -A
```
