# SMS_Manager_client_android

This client will listen and report new SMS to the server (of your choice).

The report format:
```
HTTP POST
{ data:'    {                /*** The content of data is stringified JSON ***/
        "sender": "****",
        "body": "*****"
    }'
}
```

For the server to be recognized by the APP, please return JSON response `{"success":true}` when receiving
```
HTTP POST
{ data: 'Connect' }
```

Feel free to contribute using any method that you think is appropriate.
