#!/bin/sh

service postgresql start
sleep 3
psql -U postgres -d postgres -c "ALTER USER postgres WITH PASSWORD 'postgres';"
java -jar /app.jar &
sleep 3

echo "Testing PUT /map/put"
put_map_response=$(curl -s -X POST "http://localhost:8080/map/put?key=testKey&value=testValue")
if [ "$put_map_response" != "Value saved" ]; then
    echo "PUT test failed for MapController. Expected \"Value saved\", got: \"$put_map_response\""
    exit 1
fi
echo "PUT test passed for MapController"

echo "Testing GET /map/get"
get_map_response=$(curl -s -X GET "http://localhost:8080/map/get?key=testKey")
if [ "$get_map_response" != "testValue" ]; then
    echo "GET test failed for MapController. Expected \"testValue\", got: \"$get_map_response\""
    exit 1
fi
echo "GET test passed for MapController"

echo "Testing POST /db/put"
put_db_response=$(curl -s -X POST "http://localhost:8080/db/put?key=testKey&value=testValue")
if [ "$put_db_response" != "Value saved" ]; then
    echo "PUT test failed for DBController. Expected \"Value saved\", got: \"$put_db_response\""
    exit 1
fi
echo "PUT test passed for DBController"

echo "Testing GET /db/get"
get_db_response=$(curl -s -X GET "http://localhost:8080/db/get?key=testKey")
if [ "$get_db_response" != "testValue" ]; then
    echo "GET test failed for DBController. Expected \"testValue\", got: \"$get_db_response\""
    exit 1
fi
echo "GET test passed for DBController"

populate_entries() {
    endpoint=$1
    for i in $(seq 1 100); do
        key="key_$i"
        value=$(cat /dev/urandom | tr -dc A-Za-z0-9 | head -c 8)
        curl -s -X POST "http://localhost:8080/${endpoint}/put?key=${key}&value=${value}" > /dev/null
    done
}

echo "Pre-populating MapController"
populate_entries "map"
echo "Pre-populating DBController"
populate_entries "db"
echo "You are now free to load test both controllers via requesting localhost:8080"

wait