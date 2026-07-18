package main

import (
	"crypto/tls"
	"fmt"
	"net/http"
	"sync"
	"time"
)

const (
	apiURL          = "https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com/"
	numUsers        = 10 // Number of concurrent users
	requestsPerUser = 10 // Number of requests per user
)

var httpClient = &http.Client{
	Transport: &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true}, // Ignore SSL certs
	},
	Timeout: 10 * time.Second,
}

type Stats struct {
	mu            sync.Mutex
	totalRequests int
	successCount  int
	failCount     int
	totalDuration time.Duration
	minDuration   time.Duration
	maxDuration   time.Duration
}

func (s *Stats) Record(duration time.Duration, success bool) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.totalRequests++
	if success {
		s.successCount++
		s.totalDuration += duration
		if s.minDuration == 0 || duration < s.minDuration {
			s.minDuration = duration
		}
		if duration > s.maxDuration {
			s.maxDuration = duration
		}
	} else {
		s.failCount++
	}
}

func main() {
	var wg sync.WaitGroup
	stats := &Stats{}
	start := time.Now()

	for i := 0; i < numUsers; i++ {
		wg.Add(1)
		go func(userID int) {
			defer wg.Done()
			for j := 0; j < requestsPerUser; j++ {
				startTime := time.Now()
				resp, err := httpClient.Get(apiURL)
				duration := time.Since(startTime)

				if err != nil {
					fmt.Printf("User %d - Request %d failed: %v\n", userID, j+1, err)
					stats.Record(duration, false)
					continue
				}
				resp.Body.Close()
				fmt.Printf("User %d - Request %d status: %s (%.2f ms)\n", userID, j+1, resp.Status, float64(duration.Microseconds())/1000)
				stats.Record(duration, true)
			}
		}(i)
	}

	wg.Wait()
	totalTime := time.Since(start)

	// Calculate average
	avgDuration := time.Duration(0)
	if stats.successCount > 0 {
		avgDuration = stats.totalDuration / time.Duration(stats.successCount)
	}

	// Final stats
	fmt.Println("\n===== Stress Test Summary =====")
	fmt.Printf("Total Requests:      %d\n", stats.totalRequests)
	fmt.Printf("Successful Requests: %d\n", stats.successCount)
	fmt.Printf("Failed Requests:     %d\n", stats.failCount)
	fmt.Printf("Total Time Taken:    %s\n", totalTime)
	fmt.Printf("Average Response:    %.2f ms\n", float64(avgDuration.Microseconds())/1000)
	fmt.Printf("Fastest Response:    %.2f ms\n", float64(stats.minDuration.Microseconds())/1000)
	fmt.Printf("Slowest Response:    %.2f ms\n", float64(stats.maxDuration.Microseconds())/1000)
	fmt.Println("================================")
}
