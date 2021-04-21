const AWS = require('aws-sdk')
AWS.config.update({ region: process.env.AWS_REGION })
const s3 = new AWS.S3()

// Change this value to adjust the signed URL's expiration
const URL_EXPIRATION_SECONDS = 300

// Main Lambda entry point
exports.handler = async (event, context) => {
  var response =  await getUploadURL(event)
  var final = {
    "isBase64Encoded": true|false,
    "statusCode": 200,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Credentials': true,
    },
    
    "body": JSON.stringify(response)
};
 
  context.succeed(final)

}

var keyPath = function(){
  var today = new Date();
  var dd = ("0" + (today.getDate())).slice(-2);  
  var mm = ("0" + (today.getMonth() + 1)).slice(-2); 
  var yyyy = today.getFullYear();
  return yyyy+'/'+mm+'/'+dd;
}

const getUploadURL = async function(event) {
  console.log(event)
 // const date = date()
  //const randomID = parseInt(Math.random() * 10000000)
  //const Key = `${randomID}.jpg`//
  const keyName = event["queryStringParameters"]['keyName']
  const Key = keyPath() +'/'+ keyName
  console.log(Key)
  // Get signed URL from S3
  const s3Params = {
    Bucket: process.env.BUCKET_NAME,
    Key,
    Expires: URL_EXPIRATION_SECONDS,
    ContentType: 'image/jpeg',
  }

  console.log('Params: ', s3Params)
  const uploadURL = await s3.getSignedUrlPromise('putObject', s3Params)

   var res = {
    "uploadURL": uploadURL,
    "Key": keyName
  }
  return res;
}

