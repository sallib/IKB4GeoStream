#!/bin/sh

./gradlew clean ci

echo "#####################################################################"
echo "#                Ikb4stream => Docker image creation                #"
echo "#                       Already end of support!                     #"
echo "#####################################################################"

#go into the root path
cd build/distributions/

#unzip file
unzip ikb4stream-1.0-SNAPSHOT-all.zip

#files copies
echo "Files copies..."
cp ikb4stream-1.0-SNAPSHOT-producer.jar ../../docker_images/producer
cp ikb4stream-1.0-SNAPSHOT-consumer.jar ../../docker_images/consumer
cp -rf resources/ ../../docker_images/producer/resources/
cp -rf resources/ ../../docker_images/consumer/resources/
cp -rf resources/ ../../docker_images/
echo "Files copied!"

# RM myself!
#rm $ROOT_PATH"/"$DOCKER_IMAGES_FOLDER"prepare_docker_images.sh"

echo "#########################"
echo "#       FINISHED!       #"
echo "#########################"


echo "###########################"
echo "#       BUILD STARTS      #"
echo "# made by ikb4stream team #"
echo "#         03/2017         #"
echo "###########################"

echo "=============================================="
echo "Docker image => producer:latest will be build."
echo "=============================================="

cd ../../docker_images/producer/

sudo docker build -t producer:latest .

echo "=============================================="
echo "Docker image => consumer:latest will be build."
echo "=============================================="

cd ../consumer/

sudo docker build -t consumer:latest .

cd ../

echo "################"
echo "#  BUILD ENDS  #"
echo "################"


sudo docker-compose up