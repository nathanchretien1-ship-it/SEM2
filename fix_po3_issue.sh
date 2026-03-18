#!/bin/bash
# Check how device callbacks are registered
grep -n -C 5 "new iHealthDevicesCallback" app/src/main/java/com/ihealth/demo/business/MainActivity.java
