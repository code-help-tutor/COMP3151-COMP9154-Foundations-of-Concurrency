import Dispatch
import Foundation

let max = 20

var sum = 0

let queue = DispatchQueue(label: "COMP3151", attributes: .concurrent)

var i = 0
queue.async{
  while(i < max/2){
    i = i + 1
    sum = sum + 1
    print(sum, "thread1")
  }

}
queue.async{
  while(i < max){
    i = i + 1
    sum = sum + 1
    print(sum, "thread2")
  }

}

sleep(1)

print(sum, "end")
