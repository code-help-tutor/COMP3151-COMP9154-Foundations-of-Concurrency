import Foundation

var i = 0
let max = 1000

var sum = 0

let thread = Thread {
    while(i < max){
      sum = sum + 1
      i = i + 1
    }
}

let thread1 = Thread {
    while(i < max){
      sum = sum + 1
      i = i + 1
    }
}

thread.start()
thread1.start()

sleep(1)

print(sum)

