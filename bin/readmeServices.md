##Service configuration

### *nix systems
#### Automated install

Run `installService.sh` provided script and fallow instructions to fill-in service name, root path of tnt4j-streams, running user and 
parser configuration path.

#### Manual install

1) Edit the script `./nix/tnt4j-streams-service.sh` and replace following tokens:

    * `<NAME>` = service's name
    * `<DESCRIPTION>` = Describe your service here (be concise)
    * Feel free to modify the LSB header, I've made default choices you may not agree with
    * `<FILE_PATH>` = Path to the TNT4J-Streams installation
    * `<USER>` = Login of the system user the script should be run as (for example `myuser`)
    * `<PARSER_CONFIG>` = Location of the parser configuration
    * `<TNT4J_PROPERTIES>` = TNT4J event sink configuration
    * `<LOG4J_PROPERTIES>` = Logger configuration

2) Copy to `/etc/init.d`:

```sh
    cp "./nix/tnt4j-streams-service.sh" "/etc/init.d/tnt4j-streams`"
    chmod +x /etc/init.d/tnt4j-streams
```

3) Start and test your service:

```sh
    service tnt4j-streams start
    service tnt4j-streams stop
```

4) Install service to be run at boot-time:

```sh
    update-rc.d tnt4j-streams defaults
```

### Windows system
#### Manual install

1) Edit the script `installService.bat` and replace following tokens:

    * `<NAME>` = service's name
    * `<DESCRIPTION>` = Describe your service here (be concise)
    * `<FILE_PATH>` = Path to the TNT4J-Streams installation
    * `<JVM_PATH>` = Path to the JMV root
    * `<PARSER_CONFIG>` = Location of the parser configuration
    * `<TNT4J_PROPERTIES>` = TNT4J event sink configuration
    * `<LOG4J_PROPERTIES>` = Logger configuration

2) Run the modified scrip file, now you should be able to see your service as ordinary Windows services. You can start/stop/restart and 
modify it over Windows services administration tool.

##### Registry usage

In order to troubleshoot your `TNT4J-Streams` service installation, it's worth to check the registry. The basic Service definitions are 
maintained under the registry key:
```
HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\<ServiceName>
```
Additional parameters are stored in the registry at:
```
HKEY_LOCAL_MACHINE\SOFTWARE\Apache Software Foundation\ProcRun 2.0\<ServiceName>\Parameters
```
On 64-bit Windows `procrun` always uses 32-bit registry view for storing the configuration. This means that parameters will be stored 
under:
```
HKEY_LOCAL_MACHINE\SOFTWARE\Wow6432Node\Apache Software Foundation\ProcRun 2.0\<ServiceName>
```
