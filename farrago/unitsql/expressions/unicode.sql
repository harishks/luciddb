-- $Id$
-- Test Unicode data

create schema uni;

-- should fail:  unknown character set
create table uni.t1(
i int not null primary key, v varchar(10) character set "SANSKRIT");

-- should fail:  valid SQL character set, but not supported by Farrago
create table uni.t2(
i int not null primary key, v varchar(10) character set "UTF32");

-- should fail:  valid Java character set, but not supported by Farrago
create table uni.t3(
i int not null primary key, v varchar(10) character set "UTF-8");

-- should succeed:  standard singlebyte
create table uni.t4(
i int not null primary key, v varchar(10) character set "ISO-8859-1");

-- should succeed:  alias for ISO-8859-1
create table uni.t5(
i int not null primary key, v varchar(10) character set "LATIN1");

insert into uni.t5 values (1, 'Hi');

select cast(v as varchar(1) character set "LATIN1") from uni.t5;

-- should succeed:  2-byte Unicode
create table uni.t6(
i int not null primary key, v varchar(10) character set "UTF16");

insert into uni.t6 values (1, U&'Hi');

select * from uni.t6;

select cast(v as varchar(1) character set "UTF16") from uni.t6;

-- should fail:  unknown character set
select cast(v as varchar(1) character set "SANSKRIT") from uni.t6;

select cast(v as varchar(1) character set "UTF16") from uni.t5;

select cast(v as varchar(1) character set "LATIN1") from uni.t6;

select cast(v as char(40) character set "LATIN1") from uni.t5;

select cast(v as char(40) character set "UTF16") from uni.t6;

select cast(v as char(40) character set "UTF16") from uni.t5;

select cast(v as char(40) character set "LATIN1") from uni.t6;

select char_length(v) from uni.t5;

select char_length(v) from uni.t6;

select v||v from uni.t5;

select v||v from uni.t6;

select substring(v from 1 for 1) from uni.t5;

select substring(v from 1 for 1) from uni.t6;

select substring(v from 2 for 1) from uni.t5;

select substring(v from 2 for 1) from uni.t6;

select substring(v from 2) from uni.t5;

select substring(v from 2) from uni.t6;

select overlay(v placing 'a' from 2 for 1) from uni.t5;

-- should fail:  character set mismatch
select overlay(v placing U&'a' from 2 for 1) from uni.t5;

select overlay(v placing U&'a' from 2 for 1) from uni.t6;

select overlay(v placing 'ya' from 3 for 0) from uni.t5;

select overlay(v placing U&'ya' from 3 for 0) from uni.t6;

select position('i' in v) from uni.t5;

-- FIXME:  should fail:  character set mismatch
-- select position(U&'i' in v) from uni.t5;

select position(U&'i' in v) from uni.t6;

-- test explicit character set specification
select position(_UTF16'i' in v) from uni.t6;

select trim(both from '  a  ') from uni.t5;

-- FIXME:  implicit trim char should match character set automatically
-- select trim(both from U&'  a  ') from uni.t6;

select trim(both U&' ' from U&'  a  ') from uni.t6;

select upper(v) from uni.t5;

select upper(v) from uni.t6;

select lower(v) from uni.t5;

select lower(v) from uni.t6;

select initcap(v||v) from uni.t5;

select initcap(v||v) from uni.t6;

-- for comparison ops, first test with Fennel ReshapeExecStream

select * from uni.t5 where v = 'Hi';

-- should fail:  character set mismatch
select * from uni.t6 where v = 'Hi';

select * from uni.t6 where v = U&'Hi';

select * from uni.t6 where v = U&'Hi  ';

select * from uni.t6 where v = U&'hi';

select * from uni.t6 where v > U&'H';

select * from uni.t6 where v >= U&'H';

select * from uni.t6 where v >= U&'Hi';

select * from uni.t6 where v >= U&'Ice';

select * from uni.t6 where v < U&'H';

select * from uni.t6 where v <= U&'Ice';

select * from uni.t6 where v <> U&'Hi';

select * from uni.t6 where v <> U&'bye';

-- retest comparison ops with Java calc (concat operator prevents
-- usage of ReshapeExecStream)

select * from uni.t5 where v||'' = 'Hi';

select * from uni.t6 where v||U&'' = U&'Hi';

select * from uni.t6 where v||U&'' = U&'Hi  ';

select * from uni.t6 where v||U&'' = U&'hi';

select * from uni.t6 where v||U&'' > U&'H';

select * from uni.t6 where v||U&'' >= U&'H';

select * from uni.t6 where v||U&'' >= U&'Hi';

select * from uni.t6 where v||U&'' >= U&'Ice';

select * from uni.t6 where v||U&'' < U&'H';

select * from uni.t6 where v||U&'' <= U&'Ice';

select * from uni.t6 where v||U&'' <> U&'Hi';

select * from uni.t6 where v||U&'' <> U&'bye';


-- test pattern matching

select * from uni.t5 where v like 'H%';

select * from uni.t5 where v like 'I%';

select * from uni.t5 where v like '_i';

select * from uni.t5 where v like '_a';

select * from uni.t6 where v like U&'H%';

select * from uni.t6 where v like U&'I%';

select * from uni.t6 where v like U&'_i';

select * from uni.t6 where v like U&'_a';

-- should fail:  character set mismatch

select * from uni.t6 where v like 'H%';


-- test conversion

values cast(U&'123' as int);

values cast(0.0 as varchar(128) character set "UTF16");

values cast(1.2e80 as varchar(128) character set "UTF16");

values cast(0 as varchar(128) character set "UTF16");

values cast(123.45 as varchar(128) character set "UTF16");

values cast(true as varchar(128) character set "UTF16");

values cast(date '1994-09-08' as varchar(128) character set "UTF16");

select cast(v as int) from uni.t6;


-- now insert some real Unicode data, and retest most operators;
-- note that we avoid directly retrieving Unicode so that the .ref file can
-- stay as plain ASCII

-- NOTE:  upper/lower/initcap do not seem to work on Greek letters; need
-- to find some other script where they work

insert into uni.t6 values (2, U&'\03B1\03BD\03B8\03C1\03C9\03C0\03BF\03C2');

create or replace function uni.convert_to_escaped(
s varchar(1024) character set "UTF16")
returns varchar(1024)
language java
no sql
external name 
'class net.sf.farrago.test.FarragoTestUDR.convertUnicodeToEscapedForm';

select uni.convert_to_escaped(v) from uni.t6 order by 1;

select char_length(v) from uni.t6 where i=2;

select uni.convert_to_escaped(v||v) from uni.t6 where i=2;

select uni.convert_to_escaped(substring(v from 1 for 1)) from uni.t6
where i=2;

select uni.convert_to_escaped(substring(v from 2 for 3)) from uni.t6
where i=2;

select uni.convert_to_escaped(substring(v from 2)) from uni.t6
where i=2;

select uni.convert_to_escaped(overlay(v placing U&'n' from 2 for 1)) from uni.t6
where i=2;

select position(U&'\03C1' in v) from uni.t6 where i=2;

-- test UESCAPE
select position(U&'!03C1' UESCAPE '!' in v) from uni.t6 where i=2;

select uni.convert_to_escaped(trim(both U&' ' from U&' \03B1\03BD '))
from uni.t6 where i=2;

select count(*) from uni.t6 where i=2 and v > U&'\03B1';

select count(*) from uni.t6 where i=2 and v > U&'\03B3';

select count(*) from uni.t6 
where i=2 and v = U&'\03B1\03BD\03B8\03C1\03C9\03C0\03BF\03C2';

select count(*) from uni.t6 
where i=2 and v = U&'avast ye';