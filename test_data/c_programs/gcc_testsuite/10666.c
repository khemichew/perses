

struct X
{
  int i;
};
struct Y
{
  struct X *p;
  int i;
};
extern void abort (void);
extern void foo(struct Y *);
int __attribute__((noinline)) bar(struct Y *p)
{
  p->i = 0;
  foo (p);
  return p->i;
}
int main()
{
  struct Y y;
  if (bar (&y) != 1)
    abort ();
  return 0;
}
