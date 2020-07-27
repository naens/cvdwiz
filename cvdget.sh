#! /bin/dash

sqlfile=cvd.sql

country=$1
date_from=$2
date_to=$3

if [ -z "$date_from" -o -z "$country" ]
then
    exit
fi

if [ -z "$date_to" ]
then
    date_to=$date_from
fi

sql="select day, new_cases from data
    where day >= '$date_from'
    and day <= '$date_to'
    and country = (select id from country where name = '$country');"

echo $sql | sqlite3 $sqlfile
