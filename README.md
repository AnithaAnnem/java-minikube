# ArgoCD GitOps Deployment

## Overview

ArgoCD is a **Kubernetes-native continuous deployment (CD) tool** that implements GitOps principles. It monitors your Git repository and ensures that your Kubernetes cluster always matches the desired state defined in Git.

**Key Point:** ArgoCD makes Git the **single source of truth** for your deployments.


---

## Table of Contents

- [What is ArgoCD](#what-is-argocd)  
- [Why Use ArgoCD](#why-use-argocd)  
- [CI/CD Workflow](#cicd-workflow)  
  - [Without Kubernetes](#without-kubernetes)  
  - [With Kubernetes & ArgoCD](#with-kubernetes--argocd)  
- [Benefits of ArgoCD](#benefits-of-argocd)  
- [Limitations](#limitations)  
- [Installing & Configuring ArgoCD](#installing--configuring-argocd)  
- [ArgoCD CLI Commands](#argocd-cli-commands)  
- [References](#references)

---

# What is ArgoCD

- ArgoCD is a **declarative continuous deployment tool for Kubernetes**.  
- It continuously monitors Git repositories and synchronizes the Kubernetes cluster state with the desired state defined in Git.  
- Provides a **UI, CLI, and API** to manage applications.  
- Implements **GitOps principles**, enabling automated, reliable, and predictable deployments.  


---

# Why Use ArgoCD

- Ensures **Git as the single source of truth**.  
- Automates CD pipelines for Kubernetes applications.  
- Enables **easy rollbacks** and disaster recovery.  
- Provides **cluster access control** using Git and ArgoCD RBAC.  
- Acts as a **Kubernetes extension**, integrating with existing cluster setups.  

**Timestamps in video:** 9:34 – 16:52

---

# CI/CD Workflow

### Without Kubernetes
Traditional CI/CD pipeline without Kubernetes:

- Developers commit code to Git.  
- CI pipeline builds and tests the application.  
- Deployment is done manually or via scripts.  
- Rollbacks are manual and error-prone.

### With Kubernetes & ArgoCD
- Developers commit code and/or Kubernetes manifests (YAML/Helm) to Git.  
- CI pipeline builds artifacts and updates manifests or Helm charts in Git.  
- ArgoCD continuously monitors Git and syncs the Kubernetes cluster automatically.  
- Rollbacks are simple: revert Git commits to restore previous state.  
- Provides **UI monitoring, sync status, and health checks**.

  ## Benefits of ArgoCD

- **Git as Single Source of Truth** – All cluster configurations are version-controlled in Git.  
- **Easy Rollback** – Restore previous versions by reverting Git commits.  
- **Cluster Disaster Recovery** – Quickly restore cluster state from Git.  
- **Access Control** – Fine-grained RBAC via Git and ArgoCD.  
- **Kubernetes Extension** – Integrates with clusters without replacing them.  
- **Continuous Sync** – Ensures cluster state matches Git at all times.  
- **UI & CLI** – Easy monitoring and management of applications.

# ArgoCD CLI Commands
```
# Login to ArgoCD
argocd login <ARGOCD_SERVER> --username admin --password <PASSWORD>

# List all applications
argocd app list

# Sync an application
argocd app sync <APP_NAME>

# Get application status
argocd app get <APP_NAME>
```
# References
1. [**ArgoCD Official Documentation**](https://argo-cd.readthedocs.io/)  
   - The official ArgoCD documentation provides complete guidance on installation, configuration, application deployment, CLI commands, GitOps concepts, and best practices.

2. [**TechWorld with Nana – ArgoCD Tutorial**](https://www.youtube.com/watch?v=MeU5_k9ssrs&t=45s)  
   - A beginner-friendly video tutorial explaining ArgoCD concepts, CI/CD workflow with Kubernetes, setup steps, and practical GitOps examples.
Traditional CI/CD pipeline without Kubernetes:

