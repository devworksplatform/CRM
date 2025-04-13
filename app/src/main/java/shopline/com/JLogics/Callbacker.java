package shopline.com.JLogics;

public class Callbacker {

    public static class Timer {
        public void onEnd() {

        }
    }


    public static abstract class onAnimateUpdate {

        public abstract void onUpdate(int _value);
        public abstract void onEnd();
    }



    public static class ApiResponseWaiters {
        public static class QueryApiCallback {
            public void onReceived(Business.QueryApiClient.QueryApiResponse response) {

            }
        }
        public static class BulkDetailsApiCallback {
            public void onReceived(Business.BulkDetailsApiClient.BulkDetailsApiResponse response) {

            }
        }

        public static class OrderCheckoutApiCallback {
            public void onReceived(Business.OrderCheckoutApiClient.OrderCheckoutApiResponse response) {

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
