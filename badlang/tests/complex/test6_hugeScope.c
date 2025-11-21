#include <stdio.h>

int x = 100;
int y = 200;
int z = 300;

int f0(int x) {
    x = x + 1;
    int y = x * 2;
    printf("%d\n", x);
    printf("%d\n", y);
    printf("%d\n", z);
    {
        int x = y + 10;
        int z = x + 5;
        printf("%d\n", x);
        printf("%d\n", y);
        printf("%d\n", z);
    }
    printf("%d\n", x);
    printf("%d\n", y);
    printf("%d\n", z);
    return x + y + z;
}

int f1() {
    printf("%d\n", x);
    printf("%d\n", y);
    printf("%d\n", z);
    int x = 5;
    int y = 10;
    int z = 15;
    printf("%d\n", x);
    printf("%d\n", y);
    printf("%d\n", z);
    {
        int x = 20;
        int y = 25;
        int z = 30;
        printf("%d\n", x);
        printf("%d\n", y);
        printf("%d\n", z);
    }
    printf("%d\n", x);
    printf("%d\n", y);
    printf("%d\n", z);
    return x + y + z;
}

int main() {
    printf("%d\n", x);
    printf("%d\n", y);
    printf("%d\n", z);
    int x = 1;
    int y = 2;
    int z = 3;
    printf("%d\n", x);
    printf("%d\n", y);
    printf("%d\n", z);
    int a = f0(x);
    printf("%d\n", a);
    int b = f1();
    printf("%d\n", b);
    printf("%d\n", x);
    printf("%d\n", y);
    printf("%d\n", z);
    return 0;
}
