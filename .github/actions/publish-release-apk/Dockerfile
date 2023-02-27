FROM ubuntu:20.04

RUN apt update \
	&& apt -y upgrade \
	&& apt install -y hub \
	&& apt autoremove \
	&& apt autoclean \
	&& apt clean

ADD entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
