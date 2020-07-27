#!/bin/dash

sqlfile=cvd.sql

data=$(cat tmp/owid-covid-data.json)

jq_args="'to_entries | map( \
    \"#\" + .key + \"#,\" + \
    \"#\" + (.value.location | tostring) + \"#,\" + \
    \"#\" + (.value.continent | tostring) + \"#,\" + \
    \"#\" + (.value.population | tostring) + \"#\n\") | .[]'"
values=$(echo "$data" | eval "jq -r $jq_args")

echo "$values" | tr '#' \" | while IFS= read -r line; do
    if [ -n "$line" ]
    then
        sql="insert or replace into \
            country (key,name,continent,population)
            values ($line)"
        echo $sql | sqlite3 $sqlfile
    fi
done

echo 'select * from country;' | sqlite3 $sqlfile
