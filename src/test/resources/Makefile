.PHONY = permissions archives compress clean

CPF = compress.txt
AR  = archive

all: permissions archives compress

permissions:
	cd $(AR); \
	chmod 755 permissions/executable_file.txt; \
	chmod 700 permissions/private_executable_file.txt; \
	chmod 444 permissions/readonly_file.txt; \
	chmod 700 permissions/private_folder; \
	chmod 600 permissions/private_folder/private_file.txt;

## archives

archives: c_7z c_ar c_bz2 c_cpio c_jar c_tar c_tgz c_txz c_zip

c_7z:
	cd $(AR);                          \
	7z a -t7z ../$(AR).7z . > /dev/null;

c_ar:
	cd $(AR); \
	ar cr ../$(AR).ar `find * -type f`;

c_bz2:
	cd $(AR);                 \
	tar cjf ../$(AR).tar.bz2 *;

c_cpio:
	cd $(AR);                                                         \
	find . -depth ! -path . -print | cpio --quiet -o > ../$(AR).cpio;

c_jar:
	cd $(AR);             \
	jar cMf ../$(AR).jar .;

c_tar:
	cd $(AR);           \
	tar cf ../$(AR).tar *;

c_tgz:
	cd $(AR);                \
	tar czf ../$(AR).tar.gz *;

c_txz:
	cd $(AR);               \
	tar cJf ../$(AR).tar.xz *;

c_zip:
	cd $(AR);           \
	zip -q -r ../$(AR) .;

## compress
compress: $(CPF).bz2 $(CPF).gz $(CPF).lzma $(CPF).xz

$(CPF).bz2:
	bzip2 -c $(CPF) > $@

$(CPF).gz:
	gzip -c $(CPF) > $@

$(CPF).lzma:
	lzma -z -c $(CPF) > $@

$(CPF).xz:
	xz -z -c $(CPF) > $@

## clean
clean: clean-archives clean-compress

clean-archives:
	rm -f $(AR).*

clean-compress:
	rm -f $(CPF).*
