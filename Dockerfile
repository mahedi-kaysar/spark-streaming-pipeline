## build our image on top of Debian latest version
FROM ubuntu:18.04
LABEL maintainer="md.mahedi.kaysar@gmail.com"
## initial directory
WORKDIR /home/
## install necessary packages
RUN apt-get update && apt-get install -y curl vim wget software-properties-common ssh net-tools
#RUN apt-get install -y python3 python3-pip python3-numpy python3-matplotlib python3-scipy python3-pandas python3-simpy
## set python3 as default (currently python 3.5)
#RUN update-alternatives --install "/usr/bin/python" "python" "$(which python3)" 1
## move files from downloads folder at the host to /home in the container
## comment this line if you prefer to download during the build process
## uncomment next two lines if you prefer to download during the build process
#RUN curl -L -b "oraclelicense=a" -O https://download.oracle.com/otn/java/jdk/8u261-b12/a4634525489241b9a9e1aa73d9e118e6/jdk-8u261-linux-x64.tar.gz?xd_co_f=9050760a-0aca-4d1c-9ae2-8826fbb1f9fa
RUN curl -L -O http://apache.mirror.anlx.net/spark/spark-3.0.1/spark-3.0.1-bin-hadoop2.7.tgz
RUN apt-get install -y openjdk-8-jdk
## configure java jdk 8
#RUN mkdir -p /usr/local/oracle-java-8
#RUN file jdk-8u261-linux-x64.tar.gz?xd_co_f=9050760a-0aca-4d1c-9ae2-8826fbb1f9fa
#RUN tar xvzf jdk-8u261-linux-x64.tar.gz -C /usr/local/oracle-java-8/
#RUN rm  jdk-8u261-linux-x64.tar.gz
RUN java -version
#RUN update-alternatives --install "/usr/bin/java" "java" "/usr/local/oracle-java-8/jdk1.8.0_191/bin/java" 1
#RUN update-alternatives --install "/usr/bin/javac" "javac" "/usr/local/oracle-java-8/jdk1.8.0_191/bin/javac" 1
#RUN update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/local/oracle-java-8/jdk1.8.0_191/bin/javaws" 1
#ENV JAVA_HOME="/usr/local/oracle-java-8/jdk1.8.0_191"
## configure spark
RUN mkdir -p /usr/local/spark-3.0.1
RUN tar -zxf spark-3.0.1-bin-hadoop2.7.tgz -C /usr/local/spark-3.0.1/
RUN rm spark-3.0.1-bin-hadoop2.7.tgz
RUN update-alternatives --install "/usr/sbin/start-master" "start-master" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/start-master.sh" 1
RUN update-alternatives --install "/usr/sbin/start-slave" "start-slave" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/start-slave.sh" 1
RUN update-alternatives --install "/usr/sbin/start-slaves" "start-slaves" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/start-slaves.sh" 1
RUN update-alternatives --install "/usr/sbin/start-all" "start-all" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/start-all.sh" 1
RUN update-alternatives --install "/usr/sbin/stop-all" "stop-all" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/stop-all.sh" 1
RUN update-alternatives --install "/usr/sbin/stop-master" "stop-master" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/stop-master.sh" 1
RUN update-alternatives --install "/usr/sbin/stop-slaves" "stop-slaves" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/stop-slaves.sh" 1
RUN update-alternatives --install "/usr/sbin/stop-slave" "stop-slave" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/stop-slave.sh" 1
RUN update-alternatives --install "/usr/sbin/spark-daemon.sh" "spark-daemon.sh" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/spark-daemon.sh" 1
RUN update-alternatives --install "/usr/sbin/spark-config.sh" "spark-config.sh" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/sbin/spark-config.sh" 1
RUN update-alternatives --install "/usr/bin/spark-shell" "spark-shell" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/bin/spark-shell" 1
RUN update-alternatives --install "/usr/bin/spark-class" "spark-class" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/bin/spark-class" 1
RUN update-alternatives --install "/usr/bin/spark-sql" "spark-sql" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/bin/spark-sql" 1
RUN update-alternatives --install "/usr/bin/spark-submit" "spark-submit" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/bin/spark-submit" 1
RUN update-alternatives --install "/usr/bin/pyspark" "pyspark" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/bin/pyspark" 1
RUN update-alternatives --install "/usr/bin/load-spark-env.sh" "load-spark-env.sh" "/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7/bin/load-spark-env.sh" 1
ENV SPARK_HOME="/usr/local/spark-3.0.1/spark-3.0.1-bin-hadoop2.7"
## expose for ssh
EXPOSE 22
## expose for spark use
EXPOSE 7000-8000
## expose for master webui
EXPOSE 8080
## expose for slave webui
EXPOSE 8081

COPY target/StreamingPipeline-1.0-SNAPSHOT-jar-with-dependencies.jar /home/

RUN apt-get install -y python3 python3-pip
## set python3 as default (currently python 3.5)
RUN update-alternatives --install "/usr/bin/python" "python" "$(which python3)" 1
RUN pip3 install kafka-python
COPY python/meetup_producer.py /home/meetup_producer.py
COPY python/meetup_consumber.py /home/meetup_consumber.py
RUN python --version
RUN pip3 install websockets
