# IndexServer
IndexServer is a non-blocking, thread-safe, parallel Restful API for index management, written in java. It is specifically designed for low-latency applications.


## The code
IndexServer can create indices, and add or remove shares to/from them. Dividend payouts can also be managed by IndexServer.
The status of the indices are delivarble in JSON format. IndexServer is a in-memory solution, and does usde databases.

IndexServer code consists of the follwoing classes:
1.	HttpHandler: which manages the connections and answer to the requests. All comminucations use HTTP protocol, and the required data are send/received in JSON fromat.
2.	IndexHolder: which manages the indexes.
3.	Index: which handle each index and its shares.
4.	Share: which keep the shares data.


## Usage
Just run IndexServer.jar located in IndexServer directory as:

```bash
java -jar --enable-preview IndexServer.jar
```

After run, you can send the requests to the default port of 54543.



## Assumptions
* All shares are always US dollar denominated.
* APIs might be called in parallel.
* Re-adjustment are always performed in a uniform manner.
* Each Index should have at least two members.
* Index/share name cannot be blank or null.
* Share price, the number of shares, and the dividend are always positive.
* Dividend cannot be greater than the price of the share.


## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
Please make sure to update tests as appropriate.
The code still lacks the following features, which are being added gradually:

1.	A Grphical User Interface (GUI).
2.	Increasing the performance.
3.	Increasing the number of supported APIs.


## Tests
In the test directory, a simple multi-threaded http client is implmented to check the integrity of the IndexServer. You can also use curl or any other client to test IndexServer performance.


## APIs
### Index creation API
Accepts a list of shares and creates an index:

POST /create

Returns: HTTP response with empty body.
* 201 – if index is created
* 409 – if index already exists
* 400 – if any validation fails


### Index adjustment API
Three different types of operations on one or more given indices is availabe. For any index adjustment the index value is always unchanged:

POST /indexAdjustment


1.	Share addition
Given a share with name, price and number is added to given index/indices keeping the index value constant. Moreover, the index composition is adjusted. Addition of existing member is not allowed. One request will have only one index affected.

Returns: Empty body with HTTP code:
* 201 - if member is added
* 202 - if share already exists
* 400 – if any validation fails
* 404 – index does not exist


2.	Share deletion
Given a share name, is is deleted from the given index/indices. One request will have only one index affected.

Returns: Empty body with HTTP code:
* 200 – if member is deleted
* 400 – if any validation fails
* 401 – if member is not found in the index
* 404 – index does not exist
* 405 – index has less than two members after deletion


3.	Share dividend
Adjusts the price of the given share based on dividend and re-adjusts the index composition. One request will have multiple indices affected.

Returns: Empty body with HTTP code:
* 200 – if member is updated
* 400 – if any validation fails
* 401 – if member is not found in any index


### Index state all
The latest state of all indices is returned in JSON format:

GET /indexState


### Single index state
The latest state of index is returned in JSON format.

GET /indexState/{index_name}


## Quotes
“Every problem is a gift. Without them we wouldn’t grow” – Tony Robbins.

I never refuse a good problem, which MCPricer was surely one of them. When I have a challenge, I feel alive. How abou you?
 
  
## License
[GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.en.html)
