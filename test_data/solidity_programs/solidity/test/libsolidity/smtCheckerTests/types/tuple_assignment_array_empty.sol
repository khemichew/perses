pragma experimental SMTChecker;

contract C
{
	uint[] a;
	function f(uint x) public {
		a.push(x);
	}
	function g(uint x, uint y) public {
		require(x < a.length);
		require(y < a.length);
		require(x != y);
		(, a[y]) = (2, 4);
		assert(a[x] == 2);
		assert(a[y] == 4);
	}
}
// ====
// SMTIgnoreCex: yes
// ----
// Warning 6328: (231-248): CHC: Assertion violation happens here.
