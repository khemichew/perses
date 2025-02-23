


typedef struct S
{
  int add_offset;
  int (*call)(int);
} S;

extern const S *es;

static int __attribute__((noinline))
foo (const S f, int x)
{
  es = &f;
  x = f.call(x+f.add_offset);
  x = f.call(x);
  x = f.call(x);
  return x;
}

static int
sq (int x)
{
  return x * x;
}

static const S s = {16, sq};

int
h (int x)
{
  return foo (s, x);
}
