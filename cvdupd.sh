#! /bin/dash

sqlfile=cvd.sql

data=$(cat tmp/owid-covid-data.json)

day_from=$1
day_to=$2

if [ -z $day_from ]
then
    exit
fi

if [ -z $day_to ]
then
    day_to=$day_from
fi

jq_args="'to_entries | map(.key as \$key | .value.data | map(select(.date >= \"$day_from\" and .date <= \"$day_to\") |\
    {\"key\": \$key, \"day\": .date, \"new_cases\": .new_cases})) \
    | .[] | .[] | (\
    \"#\" + .day + \"#,\" + \
    \"(select id from country where key = #\" + .key + \"#),\" + \
    (.new_cases | tostring))'"
values=$(echo "$data" | eval "jq -r $jq_args")

echo "$values" | tr '#' \" | while IFS= read -r line; do
    if [ -n "$line" ]
    then
        sql="insert or replace into 
            data (day, country, new_cases)
            values ($line);"
            
        echo $sql | sqlite3 $sqlfile
    fi
done
