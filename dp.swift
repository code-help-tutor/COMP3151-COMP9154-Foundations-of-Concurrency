import Dispatch

//Modified from https://github.com/raywenderlich/swift-algorithm-club/tree/master/DiningPhilosophers
// 


let numberOfPhilosophers = 4

struct ForkPair {
    static let forksSemaphore: [DispatchSemaphore] = Array(repeating: DispatchSemaphore(value: 1), count: numberOfPhilosophers + 1)

    let leftFork: DispatchSemaphore
    let rightFork: DispatchSemaphore

    init(leftIndex: Int, rightIndex: Int) {
        leftFork = ForkPair.forksSemaphore[leftIndex]
        rightFork = ForkPair.forksSemaphore[rightIndex]
    }

    func pickUp() {
        //Acquire by starting with the lower index
        leftFork.wait()
        rightFork.wait()
    }

    func putDown() {
        //The order does not matter here
        leftFork.signal()
        rightFork.signal()
    }
}
struct Philosophers {
    let forkPair: ForkPair
    let philosopherIndex: Int

    var leftIndex = -1
    var rightIndex = -1

    init(philosopherIndex: Int) {
        leftIndex = philosopherIndex
        rightIndex = philosopherIndex - 1

        if rightIndex < 0 {
            rightIndex += numberOfPhilosophers
        }

        self.forkPair = ForkPair(leftIndex: leftIndex, rightIndex: rightIndex)
        self.philosopherIndex = philosopherIndex

        print("Philosopher: \(philosopherIndex)  left: \(leftIndex)  right: \(rightIndex)")
    }

    func run() {
        while true {
            print("Acquiring forks for Philosopher: \(philosopherIndex) Left:\(leftIndex) Right:\(rightIndex)")
            forkPair.pickUp()
            print("Start Eating Philosopher: \(philosopherIndex)")
            print("Releasing forks for Philosopher: \(philosopherIndex) Left:\(leftIndex) Right:\(rightIndex)")
            forkPair.putDown()
        }
    }
}
let globalSem = DispatchSemaphore(value: 0)


let queue = DispatchQueue(label: "COMP3151", attributes: .concurrent)
for i in 0..<numberOfPhilosophers {
  queue.async {
      let p = Philosophers(philosopherIndex: i)
      p.run()
  }
}

for semaphore in ForkPair.forksSemaphore {
    semaphore.signal()
}

//Wait forever
globalSem.wait()