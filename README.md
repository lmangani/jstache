![JStache](http://i.imgur.com/Lnmza1J.png)
&nbsp;&nbsp; { "jstache" }
=======

JStache is a minimalistic/dumb TCP to HTTP Proxy specialized in wrapping bare JSON with meta headers *(_index, _type, @timestamp)* required for Bulk indexing at your favourite Elasticsearch cluster, with optional Basic Auth.

For anything serious, please do use Logstash.

---------------------

* {WILD JSON}  ---> [TCP]::__{ jstache }__::[HTTP] ---> *ElasticSearch/_Bulk*

---------------------



##### Requirements:
* Maven2 & Java

##### What does this do?
Not much. 

The proxy expects valid JSON ingress via TCP and pushes out ES-Meta ready JSON via HTTP

---------------------
*Example TCP IN:*
```
{ "element":"stronzio", "atomic":38 }
```
*Example HTTP OUT:*
```
POST /_bulk HTTP/1.1.
Content-Type: application/json.
Authorization: Basic **********************************.
Cache-Control: no-cache.
Pragma: no-cache.
Connection: keep-alive.
Content-Length: 132.
.

##
T 127.0.0.1:34952 -> 127.0.0.1:19200 [AP]
{"index":{"_index":"nprobe-2014.10.26","_type":"nProbe"}}
{ "@timestamp":"2014-10-26T21:05:01.029Z", "element":"stronzio", "atomic":38 }
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

### FAQ: 
##### Should I use this? 
* No, you should use Logstash

##### Is it safe for production?
* No, you should use Logstash

##### Does it really work?
* It works great, but you should use Logstash

##### Debug please?
* set handler.debug to anything in /usr/lib/jstache-{$version}/conf/config.properties
