#! /bin/dash

dir=$(pwd)

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

get_new_cases()
{
    local country=$1
    local confirmed=$2
    local var="confirmed_$country"
    local old_confirmed=$(eval echo \$$var)
    if [ -z "$old_confirmed" ]
    then
      old_confirmed=0
    fi
    local new_cases=$(expr $confirmed - $old_confirmed)
    eval $var=$confirmed
    echo $new_cases
}

cvd_upd ()
{
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

    curl "http://127.0.0.1:8080/update?command=update&date_from=$date_from&date_to=$date_to"
}

cvd_refresh ()
{
    curl "http://127.0.0.1:8080/update?command=refresh"
}



cvd_sql ()
{
    local sql="$1"
    psql -q -U $pg_user -d $pg_db -c "$sql"
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
    cvd_refresh
    echo "refresh done"
    curl "http://127.0.0.1:8080/update?command=update&days=$days"
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

  sql)
    cvd_sql "$1"
    ;;

  refresh)
     cvd_refresh $@
     ;;

  update)
    cvd_upd $@
    ;;

  renew) # download new data file and insert $1 last days into database
    cvd_renew $@
    ;;

  *)
    echo "unknown command $cmd"
    ;;
esac
