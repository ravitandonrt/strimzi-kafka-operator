curl -s https://raw.githubusercontent.com/coreos/prometheus-operator/master/example/rbac/prometheus-operator/prometheus-operator-deployment.yaml > prometheus-operator-deployment.yaml
curl -s https://raw.githubusercontent.com/coreos/prometheus-operator/master/example/rbac/prometheus-operator/prometheus-operator-cluster-role.yaml > prometheus-operator-cluster-role.yaml
curl -s https://raw.githubusercontent.com/coreos/prometheus-operator/master/example/rbac/prometheus-operator/prometheus-operator-cluster-role-binding.yaml > prometheus-operator-cluster-role-binding.yaml
curl -s https://raw.githubusercontent.com/coreos/prometheus-operator/master/example/rbac/prometheus-operator/prometheus-operator-service-account.yaml > prometheus-operator-service-account.yaml

sed -i '' 's/namespace: .*/namespace: default/' *.yaml

kubectl apply -f prometheus-operator-deployment.yaml
kubectl apply -f prometheus-operator-cluster-role.yaml
kubectl apply -f prometheus-operator-cluster-role-binding.yaml
kubectl apply -f prometheus-operator-service-account.yaml

kubectl apply -f strimzi-service-monitor.yaml
kubectl apply -f prometheus-rules.yaml
kubectl apply -f prometheus.yaml

#kubectl apply -f alert-manager.yaml