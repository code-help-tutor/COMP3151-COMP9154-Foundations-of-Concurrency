#include "header.h"

proctype insert() {
    wait(capacity_s);
    byte i = 0;
    int L = -1;
    int R = -1;
    do
    ::  write_lock(i);
        do
        ::  signal(capacity_s); //case where element is found to already exist
            unlock(i);
            goto end;
        ::  unlock(L); //case current is pointed at empty block
            L = i;
        ::  if
            ::  L > -1 -> //case where current is greater and there exists an empty block behind
                unlock(L);
                unlock(i);
                printf("insert element into %d", i);
                break;
            ::  L < 0 -> //case where current is greater and there does not exist an empty block behind
                R = i;
                i++;
                do
                ::  write_lock(i) //case where current is empty
                    //do stuff between R and i
                    unlock(R)
                    unlock(i)
                ::  write_lock(i) //case where filled
                    i++;
                    unlock(i)
                od;
            fi;
        od;
    od;
    end:
}

proctype member() {
    byte L = 0;
    byte R = CAPACITY - 1;
    byte M;
    lock_random_between_bound(L, R, M);
    write_lock(M);
    write_lock(L);
    write_lock(R);
    byte temp;
    do
    ::  temp = L; //case where M is less than target
        L = M;
        lock_random_between_bound(L, R, M);
        unlock(temp);
        if
        ::  L == R ->
            printf("%d does not exist\n");
            break;
        fi;
    ::  temp = R; //case where M is greater than target
        R = M;
        lock_random_between_bound(L, R, M);
        unlock(temp);
        if
        ::  L == R ->
            printf("%d does not exist\n");
            break;
        fi;
    ::  printf("%element exists\n"); //case where M is equal to target
        break;
    od;
    unlock(M);
    unlock(L);
    unlock(R);
}

proctype delete() {
    byte L = 0;
    byte R = CAPACITY - 1;
    byte M;
    lock_random_between_bound(L, R, M);
    write_lock(M);
    write_lock(L);
    write_lock(R);
    byte temp;
    do
    ::  temp = L; //case where M is less than target
        L = M;
        lock_random_between_bound(L, R, M);
        unlock(temp);
        if
        ::  L == R ->
            printf("%d does not exist\n");
            break;
        fi;
    ::  temp = R; //case where M is greater than target
        R = M;
        lock_random_between_bound(L, R, M);
        unlock(temp);
        if
        ::  L == R ->
            printf("%d does not exist\n");
            break;
        fi;
    ::  printf("%element exists, deleted\n"); //case where M is equal to target
        break;
    od;
    unlock(M);
    unlock(L);
    unlock(R);
    signal(capacity_s)
}

proctype print_sorted() {
    byte i = 0;
    do
    ::  if 
        ::  i < CAPACITY ->
            locks[i] <= READ_LOCK;
            printf("printed element %d", i);
            i++;
        ::  else ->
            break;
        fi;
    od;
}

init {
    byte i = 0;
    do
    ::  if
        ::  i == CAPACITY ->
            break;
        :: else ->
            locks[i] = UNLOCKED;
            i++;
        fi;
    od;
    run insert(); run delete(); run member(); run print_sorted();
}


//no two inserts,
//no two
