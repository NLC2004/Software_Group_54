@echo off
echo Starting TA Recruitment System v2...
echo === Seeded TA Accounts (password: 123456) ===
echo Zijie Zhang 231225731/123456
echo Zijun Song 231225270/123456
echo Siying Li 231225672/123456
echo Lingxiang Mei 231225557/123456
echo Lechen Ning 231225339/123456
echo Zhenkun Li 231225649/123456
echo === Seeded MO Accounts (password: 123456) ===
echo Zhang teacher01/123456
echo Song teacher02/123456
echo Li teacher03/123456
echo Mei teacher04/123456
echo Ning teacher05/123456
echo Li teacher06/123456
java -cp "out;lib\gson-2.10.1.jar" com.bupt.tarecruit.Main %*
