# README.md
# Title
1. # EKS Cluster Upgrade Playbook
2.
3. ## Overview
4. This document explains how to safely upgrade an Amazon EKS cluster (control plane), EKS add-ons, and worker nodes (managed node groups and self-managed nodes). It provides step-by-step commands, verification, rollback considerations, and post-upgrade tasks.
5.
6. ## Scope
7. - Upgrade control plane (Kubernetes minor version bump, e.g., 1.31 -> 1.32)
8. - Upgrade EKS add-ons (VPC CNI, kube-proxy, CoreDNS) — either as EKS Add-ons or self-managed DaemonSets
9. - Upgrade worker nodes:
10.   - Managed Node Groups (AWS managed)
11.   - Self-managed node groups / ASG based nodes
12. - Validate workloads and cluster functionality
13.
14. ## Pre-requirements / Assumptions
15. - You have `kubectl` configured and can reach the cluster: `kubectl get nodes` passes.
16. - AWS CLI configured with an IAM principal that can update EKS resources.
17. - `eksctl` installed (optional but recommended for many operations).
18. - Backups: cluster manifests stored in Git, snapshots/backups for persistent data (e.g., EBS, RDS), and `etcd`/namespaced backups if you need them (Velero or similar).
19. - Change control window and rollback plan agreed with stakeholders.
20.
21. ## High-Level Strategy
22. 1. Review Kubernetes version support & deprecations.
23. 2. Update/custom resource manifests if they use deprecated APIs.
24. 3. Upgrade EKS add-ons (CNI, kube-proxy, CoreDNS) to supported versions for target control plane.
25. 4. Upgrade control plane (AWS-managed).
26. 5. Upgrade worker nodes (managed node groups first, then self-managed).
27. 6. Validate, test, and monitor.
28.
29. ## Compatibility & Version Skew
30. - Check the EKS Kubernetes version lifecycle and ensure the target version is supported for your cluster. Note the EKS support windows (standard + extended). If your version is nearing EOL, plan earlier. 
31.
32. ## Step 0 — Quick checks (run before doing anything)
33. 1. Confirm cluster name and region:
34.    - `export CLUSTER=<CLUSTER_NAME>`  
35.    - `export AWS_REGION=<region>`
36. 2. Confirm current control plane version:
37.    - `aws eks describe-cluster --name $CLUSTER --region $AWS_REGION --query "cluster.version" --output text`
38. 3. Confirm kubectl client/server versions:
39.    - `kubectl version --short`
40. 4. List nodegroups:
41.    - Managed: `aws eks list-nodegroups --cluster-name $CLUSTER --region $AWS_REGION`
42.    - Self-managed nodes: `kubectl get nodes -o wide` and look for EC2 instance types/tags.
43. 5. Check installed add-ons and versions:
44.    - `aws eks describe-addon-versions --kubernetes-version <target-version>` (for planning)
45.    - `aws eks list-addons --cluster-name $CLUSTER --region $AWS_REGION`
46.
47. ## Step 1 — Prepare: review manifests & deprecations
48. 1. Run `kubectl get apiservices,deployments,daemonsets,statefulsets -A -o json | jq` to scan for deprecated API versions (or use `pluto` / `kube-no-trouble`).
49. 2. Update manifests to target supported APIs for the target Kubernetes version.
50. 3. Put a CI job to lint manifests with the target `kube-apiserver` schema if available.
51.
52. ## Step 2 — Backup & test plan
53. 1. Backup persistent volumes and critical data (EBS snapshots, DB backups).
54. 2. If you use Velero or similar, run a full backup: `velero backup create pre-upgrade-$(date +%F)`
55. 3. Create a canary namespace and test a simple deployment to ensure kubelet/CRD behavior after upgrade.
56.
57. ## Step 3 — Update EKS Add-ons (VPC CNI, CoreDNS, kube-proxy)
58. 1. Why first? Add-ons must be compatible with the new control plane version; updating them first reduces compatibility issues after control-plane upgrade.
59. 2. If you use EKS managed add-ons:
60.    - `aws eks update-addon --cluster-name $CLUSTER --addon-name vpc-cni --addon-version <version> --region $AWS_REGION`
61.    - `aws eks update-addon --cluster-name $CLUSTER --addon-name kube-proxy --addon-version <version> --region $AWS_REGION`
62.    - `aws eks update-addon --cluster-name $CLUSTER --addon-name coredns --addon-version <version> --region $AWS_REGION`
63. 3. If self-managed (DaemonSets), update the DaemonSet images and perform a rolling restart:
64.    - `kubectl rollout restart daemonset aws-node -n kube-system` (after changing image if needed)
65. 4. Verify add-ons are running:
66.    - `kubectl -n kube-system get pods -l k8s-app=aws-node` (for vpc-cni)
67.    - `kubectl -n kube-system get pods -l k8s-app=kube-dns` (for CoreDNS)
68.
69. ## Step 4 — Upgrade Control Plane (Kubernetes version)
70. 1. Overview: Control plane is upgraded via AWS (console or CLI/API). AWS performs control plane upgrade and kube-api changes.
71. 2. CLI example:
72.    - `aws eks update-cluster-version --name $CLUSTER --kubernetes-version <target-version> --region $AWS_REGION`
73. 3. Console: EKS → Clusters → Select cluster → Update → Kubernetes version → follow prompts.
74. 4. Monitor progress:
75.    - `aws eks describe-update --name $CLUSTER --update-id <update-id> --region $AWS_REGION`
76.    - Or watch Console status; control plane upgrade is usually minutes to an hour depending on size.
77. 5. After control plane upgrade, re-run `kubectl version --short` to see server version.
78.
79. ## Step 5 — Upgrade Managed Nodegroups
80. 1. If you use AWS Managed Node Groups:
81.    - Option A (Console): EKS → Cluster → Compute → Node groups → Select nodegroup → Update now (AMI release / Kubernetes version)
82.    - Option B (CLI): `aws eks update-nodegroup-version --cluster-name $CLUSTER --nodegroup-name <NODEGROUP> --launch-template <template> --kubernetes-version <target-version> --region $AWS_REGION`
83.    - Option C (`eksctl`): `eksctl upgrade nodegroup --cluster $CLUSTER --name <nodegroup> --region $AWS_REGION`
84. 2. Managed nodegroups perform rolling replacement of nodes. Monitor nodegroup update progress in Console or:
85.    - `aws eks describe-nodegroup --cluster-name $CLUSTER --nodegroup-name <NODEGROUP> --region $AWS_REGION`
86. 3. Verify pods are rescheduled and node readiness:
87.    - `kubectl get nodes`
88.    - `kubectl get pods -A --field-selector=status.phase!=Succeeded,status.phase!=Failed`
89.
90. ## Step 6 — Upgrade Self-Managed Nodes (if any)
91. 1. Pattern: create new ASG/launch template with new EKS-optimized AMI that supports target Kubernetes version, cordon/drain old nodes, and terminate old nodes.
92. 2. Steps (example):
93.    - Create new launch template/AMI and AutoScaling Group with appropriate kube-bootstrap user data (or use `eksctl create nodegroup` pattern).
94.    - Add nodes to cluster and ensure they join: `kubectl get nodes`
95.    - For each old node:
96.       a. `kubectl cordon <node>`
97.       b. `kubectl drain <node> --ignore-daemonsets --delete-local-data --force`
98.       c. Verify pods moved off and then terminate the EC2 instance or scale down ASG.
99. 3. Alternative: rolling replacement using `kubernetes.io/cluster/<cluster>` tag and instance lifecycle hooks to gracefully replace.
100.
101. ## Step 7 — Validation & Smoke Tests
102. 1. Run smoke tests and critical path tests (ingress, services, job execution).
103. 2. Check logs and events:
104.    - `kubectl get events -A --sort-by=.metadata.creationTimestamp`
105.    - `kubectl -n kube-system logs <pod>`
106. 3. Validate networking (CNI), DNS resolution (CoreDNS), and ingress controllers.
107. 4. Confirm metrics, logging, and monitoring dashboards are reporting correctly.
108.
109. ## Step 8 — Post-upgrade tasks
110. 1. Upgrade `kubectl` client to a compatible version for the new server.
111. 2. Update CI/CD runners or GitOps agents (ArgoCD/Flux) to use compatible `kubectl` / tooling.
112. 3. Update cluster autoscaler and other controllers to versions compatible with new Kubernetes.
113. 4. Update any node AMIs, scaling policies, and documentation.
114.
115. ## Rollback Considerations
116. 1. Control plane rollbacks are not straightforward — you cannot always revert to previous control plane version. Always assume rollback means:
117.    - Restore workload manifests to previous configuration
118.    - Restore data from backups (Velero, DB snapshots)
119.    - Recreate a cluster from backups if absolutely required
120. 2. If a nodegroup upgrade fails, you can roll back to previous nodegroup/AMI while leaving control plane unchanged.
121.
122. ## Automation (Terraform / CI)
123. - If you provision cluster with Terraform or CloudFormation, upgrade control plane via `aws_eks_cluster` resource changes (careful — check provider docs). For nodes, create new nodegroup resources and migrate workloads, then destroy old nodegroups.
124.
125. ## Common Pitfalls & Troubleshooting
126. - Deprecated API objects cause pods to fail to start after API changes — scan manifests first.
127. - Add-on versions incompatible with new Kubernetes cause networking or DNS breakage — update add-ons first.
128. - If pods stuck in `Pending` after node upgrade, check CNI plugin and subnet IP exhaustion.
129. - If auto upgrades are in place (extended support), be proactive and schedule your own upgrades first.
130.
131. ## Useful Commands Summary
132. - Describe cluster version: `aws eks describe-cluster --name $CLUSTER --region $AWS_REGION --query "cluster.version" --output text`
133. - Update control plane: `aws eks update-cluster-version --name $CLUSTER --kubernetes-version <target-version> --region $AWS_REGION`
134. - Update add-on: `aws eks update-addon --cluster-name $CLUSTER --addon-name vpc-cni --addon-version <version> --region $AWS_REGION`
135. - Update managed nodegroup (eksctl): `eksctl upgrade nodegroup --cluster $CLUSTER --name <NODEGROUP> --region $AWS_REGION`
136. - Cordon & drain: `kubectl cordon <node>` / `kubectl drain <node> --ignore-daemonsets --delete-local-data`
137.
138. ## Checklist (before / during / after)
139. - [ ] Backups completed (Velero/EBS snapshots/DB)
140. - [ ] Manifests checked for API deprecations
141. - [ ] Add-ons updated
142. - [ ] Control plane upgraded
143. - [ ] Managed nodegroups upgraded
144. - [ ] Self-managed nodes upgraded/replaced
145. - [ ] Smoke tests passed
146. - [ ] Monitoring/logging verified
147.
148. ## Notes & References
149. - Always test on a staging cluster first. Use canary apps and verify recovery.
150. - Keep an eye on EKS Kubernetes version lifecycle and EKS AMI support for node OS (AL2 vs AL2023).
151.
152. ## Contact / Escalation
153. - If upgrade causes production outage, follow runbook: page on-call, rollback attempts using backups, open support ticket with AWS if required.
154.
155. ----
156. Last updated: $(date +%F)
