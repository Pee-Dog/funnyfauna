package peedog.funnyfauna.entity.ant;

public class AntBinaryHeap {

	private AntNode[] heap = new AntNode[128];
	private int size = 0;

	public void clear() {
		for (int i = 0; i < size; i++) {
			heap[i].heapIdx = -1;
		}
		size = 0;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public void insert(AntNode n) {
		if (size == heap.length) {
			AntNode[] newHeap = new AntNode[heap.length * 2];
			System.arraycopy(heap, 0, newHeap, 0, heap.length);
			heap = newHeap;
		}

		heap[size] = n;
		n.heapIdx = size;
		bubbleUp(size++);
	}

	public AntNode pop() {
		AntNode result = heap[0];
		AntNode last = heap[--size];
		heap[size] = null;

		if (size > 0) {
			heap[0] = last;
			last.heapIdx = 0;
			bubbleDown(0);
		}

		result.heapIdx = -1;
		return result;
	}

	public void update(AntNode n) {
		bubbleUp(n.heapIdx);
		bubbleDown(n.heapIdx);
	}

	private void bubbleUp(int idx) {
		while (idx > 0) {
			int parent = (idx - 1) >> 1;
			if (heap[idx].f >= heap[parent].f) break;
			swap(idx, parent);
			idx = parent;
		}
	}

	private void bubbleDown(int idx) {
		while (true) {
			int left = (idx << 1) + 1;
			int right = left + 1;
			int smallest = idx;

			if (left < size && heap[left].f < heap[smallest].f)
				smallest = left;
			if (right < size && heap[right].f < heap[smallest].f)
				smallest = right;

			if (smallest == idx) break;
			swap(idx, smallest);
			idx = smallest;
		}
	}

	private void swap(int a, int b) {
		AntNode t = heap[a];
		heap[a] = heap[b];
		heap[b] = t;
		heap[a].heapIdx = a;
		heap[b].heapIdx = b;
	}
}
