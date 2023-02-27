FROM ubuntu:latest

RUN apt-get -y update
RUN apt-get -y install git

ADD entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
