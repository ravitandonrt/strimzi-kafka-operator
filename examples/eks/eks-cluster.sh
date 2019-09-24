eksctl create cluster \
--region ap-southeast-1 \
--vpc-public-subnets subnet-1,subnet-2,subnet-3 \
--vpc-private-subnets subnet-4,subnet-5,subnet-6 \
--name expt-strimzi-kafka \
--version 1.13 \
--nodegroup-name cluster-operators \
--node-type c5.xlarge \
--nodes 3 \
--node-ami auto \
--ssh-access \
--auto-kubeconfig

eksctl create nodegroup \
--region ap-southeast-1 \
--cluster expt-strimzi-kafka \
--version auto \
--name zookeepers \
--node-type r5.xlarge \
--node-ami auto \
--nodes 3

eksctl create nodegroup \
--region ap-southeast-1 \
--cluster expt-strimzi-kafka \
--version auto \
--name kafka-brokers \
--node-type r5.2xlarge \
--node-ami auto \
--nodes 3

eksctl create nodegroup \
--region ap-southeast-1 \
--cluster expt-strimzi-kafka \
--version auto \
--name kafka-producers \
--node-type c5.2xlarge \
--node-ami auto \
--nodes 6

aws --region ap-southeast-1 eks update-kubeconfig --name expt-strimzi-kafka

#eksctl delete cluster \
#--region ap-southeast-1 \
#--name expt-strimzi-kafka
