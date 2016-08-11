## Instalation

1. Fallow the instruction on TNT4J-Streams

2. Edit `tnt-data-source.xml` paser configuration file and change: your host (url), username and password

```
			<step name="Step 1"
				url="http://172.16.6.223/nagios/cgi-bin/nagios2json.cgi?servicestatustypes=31"
				method="GET"
				username="myUserName"
				password="myUserSecretPassword">
				<schedule-cron expression="0/15 * * * * ? *" />
			</step>
```

## Nagios setup (Linux)

Downlaod plugin: https://github.com/macskas/nagios2json

Build and install as described in documentation:

1. Run `make`
2. rename build binary to 'nagios2json.cgi' (May not need this step)
3. Copy built binary to nagios "cgi-bin" folder (may be /urr/lib/nagios/cgi-bin)


