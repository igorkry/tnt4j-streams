# Configuring Elastic Beats

* Open Elastic Beats configuration file, e.g. `metricbeat.yml`, `filebat.yml`, etc.
* Configure Beats output to Logstash:
```properties     
    # The Logstash hosts
    hosts: ["localhost:5044"]
```
 
# Running Elastic Beats stream 
 
* Configure `TNT4J-Streams` (set JKool token if not yet)
* Run `TNT4J-Streams` sample `elastic-beats` with your parser configuration
* Start `metricbeat`  
```cmd
    /metricbeat -c metricbeat.yml -e
```
 