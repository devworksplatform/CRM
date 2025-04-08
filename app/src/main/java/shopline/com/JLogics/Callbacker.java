package shopline.com.JLogics;

public class Callbacker {

    public static class ApiResponseWaiters {
        public static class QueryApiCallback {
            public void onReceived(Business.QueryApiClient.QueryApiResponse response) {

            }
        }
        public static class BulkDetailsApiCallback {
            public void onReceived(Business.BulkDetailsApiClient.BulkDetailsApiResponse response) {

            }
        }
    }

    public static class Auth {
        public void onSuccess() {

        }
        public void onError(String error) {

        }
    }
}
