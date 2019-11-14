docker stop zoo && docker rm zoo
docker run -d -p 2181:2181 --name zoo zookeeper

#Use cli inside container
#docker exec -ti zoo bash
#cd bin
#chmod +x zkCli.sh
#./zkCli.sh
