![JStache](http://i.imgur.com/Lnmza1J.png)
&nbsp;&nbsp; { "jstache" }
=======

JStache is a minimalistic/dumb TCP to HTTP Proxy specialized for wrapping bare JSON with meta headers required for Bulk indexing at your favourite Elasticsearch cluster. For anything serious, please do use Logstash.

---------------------

* {WILD JSON}  ---> [TCP]::__[jstache]__::[HTTP] ---> *ElasticSearch/_Bulk*

---------------------



##### Requirements:
* Maven2 & Java

##### What does this do?
Not much. 

The proxy expects JSON ingress via TCP and pushes out ES-Meta ready JSON via HTTP


Example TCP IN:
```
{ "element":"stronzio", "atomic":38 }
```
Example HTTP OUT:
```
POST /_bulk HTTP/1.1.
Content-Type: application/json.
Authorization: Basic **********************************.
Cache-Control: no-cache.
Pragma: no-cache.
Connection: keep-alive.
Content-Length: 102.
.

##
T 127.0.0.1:34952 -> 127.0.0.1:19200 [AP]
{"index":{"_index":"nprobe-2014.10.26","_type":"nProbe"}}
{ "element":"stronzio", "atomic":38 }
```

### Installation:
```
cd /usr/src
git clone https://github.com/lmangani/jstache.git
cd jstache
./install.sh
```

### Configuration:
```
/usr/lib/jstache-{$version}/conf/config.properties
```

### Usage:
```
/etc/init.d/jstached {start|stop|restart|force-reload|status}
```

---------------------

### FAQ: Should I use this? Is it safe for production?
* No, you should use Logstash
