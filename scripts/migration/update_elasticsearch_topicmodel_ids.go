package main
import (
    "os"
    "io"
    "io/ioutil"
    "bufio"
    "fmt"
    "log"
    "strings"
    "net/http"
    "sync"
)

var workPool = make(chan int, 25)
var wg sync.WaitGroup
var doneCount = 0

func main() {
    file, err := os.Open("document_topicmodels.txt")
    defer file.Close()

    if err != nil {
        log.Fatal(err)
        return
    }

    fileBuf := bufio.NewReader(file)
    line, isPrefix, err := fileBuf.ReadLine()
    for err == nil && !isPrefix {
        values := strings.Fields(string(line))
        docId := values[0]
        topicModelId := values[1]
        
        go updateIndex(docId, topicModelId) 

        line, isPrefix, err =  fileBuf.ReadLine()
    }

    if err != io.EOF {
        fmt.Println(err)
        return
    }
    wg.Wait()
    fmt.Print("Done! Num documents updated: %d", doneCount)
}

func updateIndex(docId string, topicModelId string) {
    wg.Add(1)
    workPool <- 1
    updateQuery := fmt.Sprintf("{\"script\" : \"ctx._source.topic_model_id = %s\"}", topicModelId)
    updateUrl := fmt.Sprintf("http://localhost:9200/pancake-smarts/document/%s/_update", docId)

    resp, httpErr := http.Post(updateUrl, "application/json", strings.NewReader(updateQuery))
    defer resp.Body.Close()
    if httpErr != nil {
        data, err := ioutil.ReadAll(resp.Body)
        if err != nil {
            log.Println(string(data))
        }
    }
    <-workPool
    wg.Done()
    doneCount += 1
}
