#! /bin/dash

db_file=cvd-data.db
web_source="https://covid.ourworldindata.org/data/owid-covid-data.json"
data_file=tmp/covid-data.json

day_start="2019-01-01"

# in: iso date, out: number of day
date_to_day ()
{
    local date=$1
    local d0=$(date +%s -d "$day_start")
    local d=$(date +%s -d "$date")
    echo "($d-$d0)/86400" | bc
}

# in: number of day, out: iso date
day_to_date ()
{
    local day=$1
    local d0=$(date +%s -d "$day_start")
    local str=$(echo "($day * 86400) + $d0" | bc)
    date +%F -d @$str
}

cvd_db_init ()
{
    sqlite3 $db_file << EOF
    CREATE TABLE IF NOT EXISTS country (
        id INTEGER PRIMARY KEY,
        key TEXT UNIQUE,
        name TEXT,
        continent TEXT,
        population INTEGER
    );

    CREATE TABLE IF NOT EXISTS data (
        day INTEGER,
        country INTEGER,
        new_cases INTEGER,
        PRIMARY KEY(day, country),
        FOREIGN KEY (country) REFERENCES country(id)
    );
EOF
}

cvd_fill_countries ()
{
    local data=$(cat "$data_file")

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
                country (key,name,continent,population) \
                values ($line)"
            echo $sql | sqlite3 $db_file
        fi
    done
}

cvd_init ()
{
    cvd_db_init && \
    cvd_fill_countries
}

cvd_get ()
{
    local country=$1
    local date_from=$2
    local date_to=$3

    if [ -z "$date_from" -o -z "$country" ]
    then
        exit
    fi

    if [ -z "$date_to" ]
    then
        date_to=$date_from
    fi

    local day_from=$(date_to_day $date_from)
    local day_to=$(date_to_day $date_to)
    local sql="select 
        date (day * 86400 + strftime('%s', '$day_start'), 'unixepoch'),
        new_cases from data
        where day >= '$day_from'
        and day <= '$day_to'
        and country = (select id from country where name = '$country');"
    echo "$sql" | sqlite3 "$db_file"
}

cvd_upd ()
{
    local data=$(cat "$data_file")

    local date_from=$1
    local date_to=$2

    if [ -z "$date_from" ]
    then
        exit
    fi

    if [ -z "$date_to" ]
    then
        date_to=$date_from
    fi

    local day_from=$(date_to_day $date_from)
    local day_to=$(date_to_day $date_to)

    jq_args="'to_entries | map(.key as \$key | .value.data \
        | map(select(.date >= \"$date_from\" and .date <= \"$date_to\") |\
        {\"key\": \$key, \"day\": .date, \"new_cases\": .new_cases})) \
        | .[] | .[] | (\
        \"(select (strftime(#%s#, #\" + .day + \"#) - strftime(#%s#, #$day_start#)) / 86400),\" + \
        \"(select id from country where key = #\" + .key + \"#),\" + \
        (.new_cases | tostring))'"
    values=$(echo "$data" | eval "jq -r $jq_args")

    echo "$values" | tr '#' \" | while IFS= read -r line; do
        if [ -n "$line" ]
        then
            local sql="insert or replace into 
                data (day, country, new_cases)
                values ($line);"
            echo $sql | sqlite3 $db_file
        fi
    done
}

cvd_sql ()
{
    local sql="$1"
    echo "$sql" | sqlite3 $db_file
}

cvd_refresh ()
{
    mkdir -p $(dirname "$data_file)")
    curl -s "$web_source" > "$data_file"
}

# commands are init, get, update, sql
cmd=$1
shift
case $cmd in

  init)
    cvd_init $@
    ;;

  get)
    cvd_get $@
    ;;

  update)
    cvd_upd $@
    ;;

  sql)
    cvd_sql "$1"
    ;;

  refresh) # refresh data file
    cvd_refresh $@
    ;;

  *)
    echo "unknown command $cmd"
    ;;
esac
