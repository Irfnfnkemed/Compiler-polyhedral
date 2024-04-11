#define bool _Bool
typedef unsigned long size_t;


int printf(const char *pattern, ...);
int sprintf(char *dest, const char *pattern, ...);
int scanf(const char *pattern, ...);
int sscanf(const char *src, const char *pattern, ...);
size_t strlen(const char *str);
int strcmp(const char *s1, const char *s2);
void *memcpy(void *dest, const void *src, size_t n);
void *malloc(size_t n);

void print(char *str) {
    printf("%s", str);
}

void println(char *str) {
    printf("%s\n", str);
}

void printInt(int n) {
    printf("%d", n);
}

void printlnInt(int n) {
    printf("%d\n", n);
}

char *getString() {
    char *buffer = malloc(256);
    scanf("%s", buffer);
    return buffer;
}

int getInt() {
    int n;
    scanf("%d", &n);
    return n;
}

char *toString(int n) {
    char *buffer = malloc(16);
    sprintf(buffer, "%d", n);
    return buffer;
}

int string_length(char *str) {
    return strlen(str);
}

char *string_substring(char *str, int left, int right) {
    int length = right - left;
    char *buffer = malloc(length + 1);
    memcpy(buffer, str + left, length);
    buffer[length] = '\0';
    return buffer;
}

int string_parseInt(char *str) {
    int n;
    sscanf(str, "%d", &n);
    return n;
}

int string_ord(char *str, int pos) {
    return (int)str[pos];
}

char *string_add(char *str1, char *str2) {
    int length1 = strlen(str1);
    int length2 = strlen(str2);
    int length = length1 + length2;
    char *buffer = malloc(length + 1);
    memcpy(buffer, str1, length1);
    memcpy(buffer + length1, str2, length2);
    buffer[length] = '\0';
    return buffer;
}

bool string_equal(char *str1, char *str2) {
    return strcmp(str1, str2) == 0;
}

bool string_notEqual(char *str1, char *str2) {
    return strcmp(str1, str2) != 0;
}

bool string_less(char *str1, char *str2) {
    return strcmp(str1, str2) < 0;
}

bool string_lessOrEqual(char *str1, char *str2) {
    return strcmp(str1, str2) <= 0;
}

bool string_greater(char *str1, char *str2) {
    return strcmp(str1, str2) > 0;
}

bool string_greaterOrEqual(char *str1, char *str2) {
    return strcmp(str1, str2) >= 0;
}

int array_size(void *arr) {
    return ((int*)arr)[-1];
}

void *_malloc(int n){ //分配4n个字节
    return malloc((size_t)(4 * n));
}
