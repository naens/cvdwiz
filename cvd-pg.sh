#! /bin/dash

web_source="https://covid.ourworldindata.org/data/owid-covid-data.json"
#web_source2="https://opendata.ecdc.europa.eu/covid19/casedistribution/json/"
dir=$(cd `dirname $0` && pwd)
data_dir="$dir/data"
data_file="$data_dir/covid-data-ourworld.json"
#data_file2="$data_dir/covid-data-europe.json"
pg_user=pgwiz
pg_db=cvd19


day_start="2019-01-01"

# in: iso date, out: number of day
date_to_day ()
{
    local date=$1
    local d0=$(date +%s -d "$day_start")
    local d=$(date +%s -d "$date")
    echo "($d-$d0+43200)/86400" | bc # +43200 for rounding correctly
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
    local sql="
    CREATE TABLE IF NOT EXISTS country (
        id SERIAL PRIMARY KEY,
        continent TEXT,
        population BIGINT
    );

    CREATE TABLE IF NOT EXISTS country_code (
        code TEXT PRIMARY KEY,
        country INTEGER,
        FOREIGN KEY (country) REFERENCES country(id)
    );

    CREATE TABLE IF NOT EXISTS country_name (
        name TEXT PRIMARY KEY,
        country INTEGER,
        FOREIGN KEY (country) REFERENCES country(id)
    );

    CREATE TABLE IF NOT EXISTS data (
        day INTEGER,
        country INTEGER,
        new_cases INTEGER,
        PRIMARY KEY(day, country),
        FOREIGN KEY (country) REFERENCES country(id)
    );"
    psql -q -U $pg_user -d $pg_db -c "$sql"
}

cvd_fill_countries ()
{
    psql -q -U $pg_user -d $pg_db -f "$dir/country_insert.sql"
}

cvd_init ()
{
    cvd_db_init && cvd_fill_countries
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
        to_char(TIMESTAMP 'epoch' + (day * 86400 + extract(epoch from timestamp '$day_start')) * INTERVAL '1 second', 'YYYY-MM-DD'),
        new_cases from data
        where day >= '$day_from'
        and day <= '$day_to'
        and country = (select id from country where name = '$country');"
    psql -q -U $pg_user -d $pg_db -c "$sql"
}

cvd_upd ()
{
    local data=$(cat "$data_file")

    local date_from=$1
    local date_to=$2

    local temp=$(mktemp)

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
        | map(select(.date >= \"$date_from\" and .date <= \"$date_to\") | \
        {\"key\": \$key, \"day\": .date, \"new_cases\": .new_cases})) \
        | .[] | .[] | ( \
        \"(select (extract(epoch from timestamp \$\$\" + .day + \"\$\$) \
            - extract(epoch from timestamp \$\$$day_start\$\$)) / 86400),\" + \
        \"(select id from country where key = \$\$\" + .key + \"\$\$),\" + \
        (.new_cases | tostring))'"
    values=$(echo "$data" | eval "jq -r $jq_args")

    echo "BEGIN TRANSACTION;" > "$temp"
    echo "$values" | while IFS= read -r line; do
        if [ -n "$line" ]
        then
            local sql="insert into data (day, country, new_cases)
                values ($line)
                on conflict (day, country) do update set
                new_cases=EXCLUDED.new_cases;"
            echo "$sql" >> "$temp"
        fi
    done
    echo "COMMIT;" >> "$temp"
    psql -q -U $pg_user -d $pg_db -f "$temp"
    rm -rf "$temp"
}

cvd_upd2 ()
{
    local data=$(cat "$data_file2")

    local date_from=$1
    local date_to=$2

    local temp=$(mktemp)

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

    jq_args="'.records | \
        map( { date: (.year + \"-\" + .month + \"-\" + .day), key: .countryterritoryCode, new_cases: .cases } \
            | select(.date >= \"$date_from\" and .date <= \"$date_to\")) \
            | .[] | ( \
                \"(select (extract(epoch from timestamp \$\$\" + .date + \"\$\$) \
                    - extract(epoch from timestamp \$\$$day_start\$\$)) / 86400),\" + \
                \"(select id from country where key = \$\$\" + .key + \"\$\$),\" + \
                (.new_cases | tostring))'"
    values=$(echo "$data" | eval "jq -r $jq_args")

    echo temp="$temp"
    echo "BEGIN TRANSACTION;" > "$temp"
    echo "$values" | while IFS= read -r line; do
        if [ -n "$line" ]
        then
            local sql="insert into data (day, country, new_cases_eu)
                values ($line)
                on conflict (day, country) do update set
                new_cases=EXCLUDED.new_cases_eu;"
            echo "$sql" >> "$temp"
            psql -q -U $pg_user -d $pg_db -c "$sql"
        fi
    done
    echo "COMMIT;" >> "$temp"
#    psql -q -U $pg_user -d $pg_db -f "$temp"
#    cat "$temp"
#    rm -rf "$temp"
}


cvd_sql ()
{
    local sql="$1"
    psql -q -U $pg_user -d $pg_db -c "$sql"
}

cvd_refresh ()
{
    mkdir -p $(dirname "$data_file)")
    curl -s "$web_source" > "$data_file"
#    curl -s "$web_source2" > "$data_file2"
}

cvd_log ()
{
    local email="cvd19@yopmail.com"
    local log_file="/tmp/cvd.log"
    echo "Log for $(date)" >> "$log_file"
    cvd_sql 'select count(*) from data' >> "$log_file"
    echo >> "$log_file"
    # TODO: mail
}

# for crontab:
# *   */8 *   *   * <path for cvd-pg.sh> renew 3
cvd_renew ()
{
    local days=$1
    if [ ! -n "$days" ]
    then
        echo "cvd_renew: bad arguments"
        return
    fi
    local date_to=$(date +%F)
    local date_from=$(day_to_date $(expr $(date_to_day $date_to) - $days))
    cvd_refresh
    echo "refresh done"
    cvd_upd $date_from $date_to
#    cvd_upd2 $date_from $date_to
    echo "update $date_from $date_to done"
    cvd_log
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

  update) # update the database from file
    cvd_upd $@
#    cvd_upd2 $@
    ;;

  sql)
    cvd_sql "$1"
    ;;

  refresh) # refresh data file
    cvd_refresh $@
    ;;

  renew) # download new data file and insert $1 last days into database
    cvd_renew $@
    ;;

  *)
    echo "unknown command $cmd"
    ;;
esac
