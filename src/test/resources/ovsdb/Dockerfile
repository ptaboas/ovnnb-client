FROM debian:wheezy-slim

ENV OPENVSWITCH_VERSION v2.7.0

RUN apt-get update -q && apt-get install -q -y wget \
	gcc make \
	perl \
	python-pip \
	git \
	autoconf libtool libcap-ng-dev

RUN pip install six

RUN git clone https://github.com/openvswitch/ovs.git \
	&& cd ovs && git checkout tags/$OPENVSWITCH_VERSION
	
WORKDIR /ovs
RUN ./boot.sh
RUN ./configure
RUN make
RUN make install

RUN ovsdb-tool create /usr/local/etc/openvswitch/ovnnb_db.db /usr/local/share/openvswitch/ovn-nb.ovsschema

ENTRYPOINT ["ovsdb-server"] 