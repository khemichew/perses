

void
a17_1_wrong ()
{
  union
  {
    int n;
    float x;
  } u;
#pragma omp parallel
 {
#pragma omp atomic
 u.n++;
#pragma omp atomic
 u.x += 1.0;


  }
}
