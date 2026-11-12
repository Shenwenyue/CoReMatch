package xxl.core.collections.containers.compression.buffer.impl;

import xxl.core.collections.containers.compression.buffer.BlockBuffer;
import xxl.core.io.raw.RawAccess;

public class NoBlockBuffer implements BlockBuffer {

	private RawAccess raf;

	private boolean ooo = false;

	public NoBlockBuffer(RawAccess raf) {
		this.raf = raf;
	}

	@Override
	public void setRawAccess(RawAccess raf) {
		this.raf = raf;
	}

	@Override
	public void getBlock(byte[] b, long blockNumber, boolean tlbLeaf) {
//		System.out.print(blockNumber + ", ");
		raf.read(b, blockNumber);
	}

	@Override
	public void replace(long blockNumber, byte[] block) {
	}

	@Override
	public void getTLBEntry(byte[] b, long blockNumber, int level, int levelIndex) {
		getBlock(b, blockNumber, false);
	}

	@Override
	public void replaceTLB(byte[] block, int level, int levelIndex) {
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public boolean isOutOfOrder() {
		return ooo;
	}

	@Override
	public void setOutOfOrder(boolean value) {
		ooo = value;
	}

	@Override
	public String logState() {
		return "Empty BlockBuffer";
	}

}
